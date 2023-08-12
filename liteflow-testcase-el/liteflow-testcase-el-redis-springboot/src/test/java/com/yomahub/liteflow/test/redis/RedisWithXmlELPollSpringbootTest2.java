package com.yomahub.liteflow.test.redis;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;

/**
 * springboot环境下的redis配置源轮询拉取模式功能测试
 *
 * @author hxinyu
 * @since 2.11.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/redis/application-poll-xml.properties")
@SpringBootTest(classes = RedisWithXmlELPollSpringbootTest2.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.redis.cmp"})
public class RedisWithXmlELPollSpringbootTest2 extends BaseTest {

    @MockBean(name = "chainJedis")
    private static Jedis chainJedis;

    @MockBean(name = "scriptJedis")
    private static Jedis scriptJedis;

    @Resource
    private FlowExecutor flowExecutor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeAll
    public static void setUpBeforeClass() {

        jedis.hset("pollScriptKey", "s11:script:脚本s11:groovy", "defaultContext.setData(\"test11\",\"hello s11\");");
        jedis.hset("pollScriptKey", "s22:script:脚本s22:js", "defaultContext.setData(\"test22\",\"hello s22\");");
        jedis.hset("pollScriptKey", "s33:script:脚本s33", "defaultContext.setData(\"test33\",\"hello s33\");");

        jedis.hset("pollChainKey", "chain11", "THEN(a, b, c);");
        jedis.hset("pollChainKey", "chain22", "THEN(a, b, c, s33);");
        jedis.hset("pollChainKey", "chain33", "THEN(a, b, c, s11, s22);");
    }

    @Test
    public void testPollWithXml() throws InterruptedException {
        LiteflowResponse response = flowExecutor.execute2Resp("chain11", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());


        //修改redis中规则
        changeXMLData();
        //重新加载规则 定时任务1分钟后开始第一次轮询 所以这里休眠1分钟
        Thread.sleep(65000);
        Assertions.assertEquals("a==>c==>b", flowExecutor.execute2Resp("chain11", "arg").getExecuteStepStr());

        //删除redis中规则
        deleteXMLData();
        //重新加载规则
        Thread.sleep(1000);
        response = flowExecutor.execute2Resp("chain11", "arg");
        Assertions.assertTrue(!response.isSuccess());
        response = flowExecutor.execute2Resp("chain22", "arg");
        Assertions.assertTrue(!response.isSuccess());

        //添加redis中规则
        addXMLData();
        //重新加载规则
        Thread.sleep(1000);
        Assertions.assertEquals("b==>c", flowExecutor.execute2Resp("chain44", "arg").getExecuteStepStr());
    }

    @Test
    public void testPollWithScriptXml() throws InterruptedException {
        LiteflowResponse response = flowExecutor.execute2Resp("chain33", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("hello s11", context.getData("test11"));
        Assertions.assertEquals("a==>b==>c==>s11[脚本s11]==>s22[脚本s22]", response.getExecuteStepStrWithoutTime());

        //添加和删除脚本
        //重新加载脚本 定时任务1分钟后开始第一次轮询 所以这里休眠1分钟
        addAndDeleteScriptData();
        Thread.sleep(62000);
        //修改redis脚本
        changeScriptData();
        Thread.sleep(1000);
        context = flowExecutor.execute2Resp("chain33", "arg").getFirstContextBean();
        Assertions.assertEquals("hello s11 version2", context.getData("test11"));
    }


    /**
     * 修改redisson中的chain
     */
    public void changeXMLData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        jedis.hset(redisParserVO.getChainKey(), "chain11", "THEN(a, c, b);");
    }

    /**
     * 删除redisson中的chain
     */
    public void deleteXMLData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        jedis.hdel(redisParserVO.getChainKey(), "chain11");
        jedis.hdel(redisParserVO.getChainKey(), "chain22");
    }


    /**
     * 新增redisson中的chain
     */
    public void addXMLData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        jedis.hset(redisParserVO.getChainKey(), "chain33", "THEN(a, b, c, s11, s22);");
        jedis.hset(redisParserVO.getChainKey(), "chain44", "THEN(b, c);");
    }

    /**
     * 修改redisson中的脚本
     */
    public void changeScriptData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        jedis.hset(redisParserVO.getScriptKey(), "s11:script:脚本s11:groovy", "defaultContext.setData(\"test11\",\"hello s11 version2\");");
    }

    /**
     * 新增和删除redisson中的chain
     */
    public void addAndDeleteScriptData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        jedis.hdel(redisParserVO.getScriptKey(), "s33:script:脚本s33");
        jedis.hset(redisParserVO.getScriptKey(),"s55:script:脚本s55:groovy", "defaultContext.setData(\"test55\",\"hello s5\");");
    }

/*  //为便于测试的redis内规则数据数据清空
    @Test
    public void testCleanData() {
        Set<String> scriptKey = jedis.hkeys("pollScriptKey");
        Set<String> chainKey = jedis.hkeys("pollChainKey");
        for (String key : scriptKey) {
            jedis.hdel("pollScriptKey", key);
        }
        for (String key : chainKey) {
            jedis.hdel("pollChainKey", key);
        }
        jedis.hkeys("pollChainKey").forEach(System.out::println);
        System.out.println(" ");
        jedis.hkeys("pollScriptKey").forEach(System.out::println);
        System.out.println("数据清空完成");
    }*/
}
