package com.yomahub.liteflow.test.redis;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RMapCache;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
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
@SpringBootTest(classes = RedisWithXmlELPollSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.redis.cmp"})
public class RedisWithXmlELPollSpringbootTest {

    private static Jedis jedis;

    @Resource
    private FlowExecutor flowExecutor;

    @BeforeAll
    public static void setUpBeforeClass() {
        jedis = new Jedis("localhost", 6379);
        jedis.select(1);
        jedis.hset("pollScriptKey", "s1:script:脚本s1:groovy", "defaultContext.setData(\"test1\",\"hello s1\");");
        jedis.hset("pollScriptKey", "s2:script:脚本s2:js", "defaultContext.setData(\"test2\",\"hello s2\");");
        jedis.hset("pollScriptKey", "s3:script:脚本s3", "defaultContext.setData(\"test3\",\"hello s3\");");

        jedis.hset("pollChainKey", "chain1", "THEN(a, b, c);");
        jedis.hset("pollChainKey", "chain2", "THEN(a, b, c, s3);");
        jedis.hset("pollChainKey", "chain3", "THEN(a, b, c, s1, s2);");
    }

    @Test
    public void testPollWithXml() throws InterruptedException {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());


        //修改redis中规则
        changeXMLData();
        //重新加载规则 定时任务1分钟后开始第一次轮询 所以这里休眠1分钟
        Thread.sleep(65000);
        Assertions.assertEquals("a==>c==>b", flowExecutor.execute2Resp("chain1", "arg").getExecuteStepStr());

        //删除redis中规则
        deleteXMLData();
        //重新加载规则
        Thread.sleep(5000);
        response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(!response.isSuccess());
        response = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertTrue(!response.isSuccess());


        //添加redis中规则
        addXMLData();
        //重新加载规则
        Thread.sleep(5000);
        Assertions.assertEquals("b==>c", flowExecutor.execute2Resp("chain4", "arg").getExecuteStepStr());
    }


    /**
     * 修改redisson中的chain
     */
    public void changeXMLData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        jedis.hset(redisParserVO.getChainKey(), "chain1", "THEN(a, c, b);");
    }

    /**
     * 删除redisson中的chain
     */
    public void deleteXMLData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        jedis.hdel(redisParserVO.getChainKey(), "chain1");
        jedis.hdel(redisParserVO.getChainKey(), "chain2");
    }


    /**
     * 新增redisson中的chain
     */
    public void addXMLData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        jedis.hset(redisParserVO.getChainKey(), "chain4", "THEN(b, c);");
    }

    /**
     * 修改redisson中的脚本
     */
    public void changeScriptData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        jedis.hset(redisParserVO.getScriptKey(), "s1:script:脚本s1:groovy", "defaultContext.setData(\"test1\",\"hello s1 version2\");");
    }
}
