package com.yomahub.liteflow.test.redis;

import cn.hutool.crypto.digest.DigestUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.redis.mode.RClient;
import com.yomahub.liteflow.parser.redis.mode.polling.RedisParserPollingMode;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * springboot环境下的redis配置源chain轮询拉取模式功能测试
 *
 * @author hxinyu
 * @since 2.11.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/redis/application-poll-xml.properties")
@SpringBootTest(classes = RedisWithXmlELPollSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.redis.cmp"})
public class RedisWithXmlELPollSpringbootTest extends BaseTest {

    @MockBean(name = "chainClient")
    private static RClient chainClient;

    @MockBean(name = "scriptClient")
    private static RClient scriptClient;

    @Resource
    private FlowExecutor flowExecutor;

    //计算hash中field数量的lua脚本
    private final String luaOfKey = "local keys = redis.call(\"hkeys\", KEYS[1]);\n" +
            "return #keys;\n";

    //计算hash中value的SHA值的lua脚本
    private final String luaOfValue = "local key = KEYS[1];\n" +
            "local field = KEYS[2];\n" +
            "local value, err = redis.call(\"hget\", key, field);\n" +
            "if value == false or value == nil then\n" +
            "    return \"nil\";\n" +
            "end\n" +
            "local sha1 = redis.sha1hex(value);\n" +
            "return sha1;";

    static LFLog LOG = LFLoggerManager.getLogger(RedisWithXmlELPollSpringbootTest.class);


    @AfterAll
    public static void after() {
        //关闭poll模式的轮询线程池
        try{
            Field pollExecutor = RedisParserPollingMode.class.getDeclaredField("pollExecutor");
            pollExecutor.setAccessible(true);
            ScheduledThreadPoolExecutor threadPoolExecutor = (ScheduledThreadPoolExecutor) pollExecutor.get(null);
            threadPoolExecutor.shutdownNow();
        } catch (Exception ignored) {
            LOG.error("[Polling thread pool not closed]", ignored);
        }
    }

    /**
     * 统一测试chain和script
     *
     * 测试数据流程：
     * 1、执行chain1值："THEN(a, b, c);"
     * 2、修改chain1值为："THEN(s11, s22, s33, a, b);", 执行新chain 验证chain的轮询拉取功能
     * 3、修改chain1其中的script11值 执行chain 验证script的轮询拉取功能
     */
    @Test
    public void testPollWithXml() throws InterruptedException {
        Set<String> chainNameSet = new HashSet<>();
        chainNameSet.add("chain11");
        String chainValue = "THEN(a, b, c);";
        String chainSHA = DigestUtil.sha1Hex(chainValue);

        //修改chain并更新SHA值
        String changeChainValue = "THEN(s11, s22, s33, a, b);";
        String changeChainSHA = DigestUtil.sha1Hex(changeChainValue);

        when(chainClient.hkeys("pollChainKey")).thenReturn(chainNameSet);
        when(chainClient.hget("pollChainKey", "chain11")).thenReturn(chainValue).thenReturn(changeChainValue);
        when(chainClient.scriptLoad(luaOfKey)).thenReturn("keysha");
        when(chainClient.scriptLoad(luaOfValue)).thenReturn("valuesha");
        when(chainClient.evalSha(eq("keysha"), anyString())).thenReturn("1");
        when(chainClient.evalSha(eq("valuesha"), anyString(), anyString())).thenReturn(chainSHA).thenReturn(changeChainSHA);

        //添加script
        Set<String> scriptFieldSet = new HashSet<>();
        scriptFieldSet.add("s11:script:脚本s11:groovy");
        scriptFieldSet.add("s22:script:脚本s22:js");
        scriptFieldSet.add("s33:script:脚本s33");
        String s11 = "defaultContext.setData(\"test11\",\"hello s11\");";
        String s22 = "defaultContext.setData(\"test22\",\"hello s22\");";
        String s33 = "defaultContext.setData(\"test33\",\"hello s33\");";
        //SHA值用于测试修改script的轮询刷新功能
        String s11SHA = DigestUtil.sha1Hex(s11);
        String s22SHA = DigestUtil.sha1Hex(s22);
        String s33SHA = DigestUtil.sha1Hex(s33);
        //修改script值并更新SHA值
        String changeS11 = "defaultContext.setData(\"test11\",\"hello world\");";
        String changeS11SHA = DigestUtil.sha1Hex(changeS11);

        when(scriptClient.hkeys("pollScriptKey")).thenReturn(scriptFieldSet);
        //这里休眠一段时间是为了防止在未修改脚本的chain还没有执行前 轮询线程就拉取了新script值
        when(scriptClient.hget("pollScriptKey", "s11:script:脚本s11:groovy")).thenReturn(s11).thenAnswer(invocation -> {
            Thread.sleep(2000);
            return changeS11;
        }).thenReturn(changeS11);
        when(scriptClient.hget("pollScriptKey", "s22:script:脚本s22:js")).thenReturn(s22);
        when(scriptClient.hget("pollScriptKey", "s33:script:脚本s33")).thenReturn(s33);

        //分别模拟三个script的evalsha指纹值计算的返回值, 其中s11脚本修改 指纹值变化
        when(scriptClient.scriptLoad(luaOfKey)).thenReturn("keysha");
        when(scriptClient.scriptLoad(luaOfValue)).thenReturn("valuesha");
        when(scriptClient.evalSha(eq("keysha"), anyString())).thenReturn("3");
        when(scriptClient.evalSha("valuesha", "pollScriptKey", "s11:script:脚本s11:groovy")).thenReturn(s11SHA).thenAnswer(invocation -> {
            Thread.sleep(2000);
            return changeS11SHA;
        }).thenReturn(changeS11SHA);
        when(scriptClient.evalSha("valuesha", "pollScriptKey", "s22:script:脚本s22:js")).thenReturn(s22SHA);
        when(scriptClient.evalSha("valuesha", "pollScriptKey", "s33:script:脚本s33")).thenReturn(s33SHA);

        //测试修改前的chain
        LiteflowResponse response = flowExecutor.execute2Resp("chain11", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());

        Thread.sleep(4000);

        //测试加了script的chain
        response = flowExecutor.execute2Resp("chain11", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("hello s11", context.getData("test11"));
        Assertions.assertEquals("hello s22", context.getData("test22"));
        Assertions.assertEquals("s11[脚本s11]==>s22[脚本s22]==>s33[脚本s33]==>a==>b", response.getExecuteStepStrWithoutTime());

        Thread.sleep(4000);

        //测试修改script后的chain
        response = flowExecutor.execute2Resp("chain11", "arg");
        context  = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("hello world", context.getData("test11"));
    }

    @Test
    public void testDisablePollWithXml() throws InterruptedException {
        Set<String> chainNameSet = new HashSet<>();
        chainNameSet.add("chain1122:false");
        String chainValue = "THEN(a, b, c);";

        when(chainClient.hkeys("pollChainKey")).thenReturn(chainNameSet);
        when(chainClient.hget("pollChainKey", "chain1122:true")).thenReturn(chainValue);

        Set<String> scriptFieldSet = new HashSet<>();
        scriptFieldSet.add("s4:script:脚本s3:groovy:false");
        when(scriptClient.hkeys("pollScriptKey")).thenReturn(scriptFieldSet);
        when(scriptClient.hget("pollScriptKey", "s4:script:脚本s3:groovy:true")).thenReturn("defaultContext.setData(\"test\",\"hello\");");

        // 测试 chain 停用
        Assertions.assertThrows(ChainNotFoundException.class, () -> {
            throw flowExecutor.execute2Resp("chain1122", "arg").getCause();
        });

        // 测试 script 停用
        Assertions.assertTrue(!FlowBus.getNodeMap().containsKey("s4"));
    }
}
