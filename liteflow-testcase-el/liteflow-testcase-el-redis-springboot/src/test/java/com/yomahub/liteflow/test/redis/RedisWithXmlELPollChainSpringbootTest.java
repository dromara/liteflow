package com.yomahub.liteflow.test.redis;

import cn.hutool.crypto.digest.DigestUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.redis.mode.RClient;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * springboot环境下的redis配置源chain轮询拉取模式功能测试
 *
 * @author hxinyu
 * @since 2.11.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/redis/application-poll-chain-xml.properties")
@SpringBootTest(classes = RedisWithXmlELPollChainSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.redis.cmp"})
public class RedisWithXmlELPollChainSpringbootTest extends BaseTest {

    @MockBean(name = "chainClient")
    private static RClient chainClient;

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

    /**
     * 测试chain
     */
    @Test
    public void testPollWithXml() throws InterruptedException {
        Set<String> chainNameSet = new HashSet<>();
        chainNameSet.add("chain11");
        String chainValue = "THEN(a, b, c);";
        //SHA值用于测试修改chain的轮询刷新功能
        String chainSHA = DigestUtil.sha1Hex(chainValue);

        //修改chain并更新SHA值
        String changeChainValue = "THEN(a, c);";
        String changeChainSHA = DigestUtil.sha1Hex(changeChainValue);
        when(chainClient.hkeys("pollChainKey")).thenReturn(chainNameSet);
        when(chainClient.hget("pollChainKey", "chain11")).thenReturn(chainValue).thenReturn(changeChainValue);
        when(chainClient.scriptLoad(luaOfKey)).thenReturn("keysha");
        when(chainClient.scriptLoad(luaOfValue)).thenReturn("valuesha");
        when(chainClient.evalSha(eq("keysha"), anyString())).thenReturn("1");
        when(chainClient.evalSha(eq("valuesha"), anyString(), anyString())).thenReturn(chainSHA).thenReturn(changeChainSHA);

        //测试修改前的chain
        LiteflowResponse response = flowExecutor.execute2Resp("chain11", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());

        Thread.sleep(4000);

        //测试修改后的chain
        response = flowExecutor.execute2Resp("chain11", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>c", response.getExecuteStepStr());
    }
}
