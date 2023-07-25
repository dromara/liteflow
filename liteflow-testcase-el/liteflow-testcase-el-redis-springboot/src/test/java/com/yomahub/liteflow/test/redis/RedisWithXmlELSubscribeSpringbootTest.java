package com.yomahub.liteflow.test.redis;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.spi.holder.SpiFactoryCleaner;
import com.yomahub.liteflow.spring.ComponentScanner;
import com.yomahub.liteflow.thread.ExecutorHelper;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.Set;

/**
 * springboot环境下的redis配置源订阅模式功能测试
 *
 * @author hxinyu
 * @since 2.11.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/redis/application-sub-xml.properties")
@SpringBootTest(classes = RedisWithXmlELSubscribeSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.redis.cmp"})
public class RedisWithXmlELSubscribeSpringbootTest {

    private static RedissonClient redissonClient;

    @Resource
    private FlowExecutor flowExecutor;

    @BeforeAll
    public static void setUpBeforeClass() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setDatabase(1);
        redissonClient = Redisson.create(config);
        RMapCache<String, String> chainKey = redissonClient.getMapCache("chainKey");
        RMapCache<String, String> scriptKey = redissonClient.getMapCache("scriptKey");
        scriptKey.put("s1:script:脚本s1:groovy", "defaultContext.setData(\"test1\",\"hello s1\");");
        scriptKey.put("s2:script:脚本s2:js", "defaultContext.setData(\"test2\",\"hello s2\");");
        scriptKey.put("s3:script:脚本s3", "defaultContext.setData(\"test3\",\"hello s3\");");
        chainKey.put("chain1", "THEN(a, b, c);");
        chainKey.put("chain2", "THEN(a, b, c, s3);");
        chainKey.put("chain3", "THEN(a, b, c, s1, s2);");
    }

    @Test
    public void testSubWithXml() throws InterruptedException {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());

        //修改redis中规则
        changeXMLData();
        //重新加载规则
        Thread.sleep(50);
        Assertions.assertEquals("a==>c==>b", flowExecutor.execute2Resp("chain1", "arg").getExecuteStepStr());

        //删除redis中规则
        deleteXMLData();
        //重新加载规则
        Thread.sleep(50);
        response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(!response.isSuccess());

        //添加redis中规则
        addXMLData();
        //重新加载规则
        Thread.sleep(50);
        Assertions.assertEquals("b==>c", flowExecutor.execute2Resp("chain4", "arg").getExecuteStepStr());
    }

    @Test
    public void testSubWithScriptXml() throws InterruptedException {
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("hello s1", context.getData("test1"));
        Assertions.assertEquals("a==>b==>c==>s1[脚本s1]==>s2[脚本s2]", response.getExecuteStepStrWithoutTime());

        //添加和删除脚本
        addAndDeleteScriptData();
        //修改redis脚本
        changeScriptData();
        Thread.sleep(50);
        context = flowExecutor.execute2Resp("chain3", "arg").getFirstContextBean();
        Assertions.assertEquals("hello s1 version2", context.getData("test1"));
        context = flowExecutor.execute2Resp("chain2", "arg").getFirstContextBean();
        Assertions.assertEquals("hello s3 version2", context.getData("test2"));
    }

    /**
     * 修改redisson中的chain
     */
    public void changeXMLData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        RMapCache<String, String> chainKey = redissonClient.getMapCache(redisParserVO.getChainKey());
        chainKey.put("chain1", "THEN(a, c, b);");
    }

    /**
     * 删除redisson中的chain
     */
    public void deleteXMLData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        RMapCache<String, String> chainKey = redissonClient.getMapCache(redisParserVO.getChainKey());
        chainKey.remove("chain1");
        chainKey.remove("chain4");
    }

    /**
     * 新增redisson中的chain
     */
    public void addXMLData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        RMapCache<String, String> chainKey = redissonClient.getMapCache(redisParserVO.getChainKey());
        chainKey.put("chain4","THEN(b, c);");
    }

    /**
     * 修改redisson中的脚本
     */
    public void changeScriptData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        RMapCache<String, String> scriptKey = redissonClient.getMapCache(redisParserVO.getScriptKey());
        scriptKey.put("s1:script:脚本s1:groovy", "defaultContext.setData(\"test1\",\"hello s1 version2\");");
        scriptKey.put("s3:script:脚本s3", "defaultContext.setData(\"test2\",\"hello s3 version2\");");
    }

    /**
     * 新增和删除redisson中的chain
     */
    public void addAndDeleteScriptData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        RMapCache<String, String> scriptKey = redissonClient.getMapCache(redisParserVO.getScriptKey());
        scriptKey.remove("s3:script:脚本s3");
        scriptKey.put("s5:script:脚本s5:groovy", "defaultContext.setData(\"test1\",\"hello s5\");");
    }

/*    //为便于测试的redis内规则数据数据清空
    @Test
    public void testCleanData(){
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        RedisParserVO redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
        RMapCache<String, String> scriptKey = redissonClient.getMapCache(redisParserVO.getScriptKey());
        RMapCache<String, String> chainKey = redissonClient.getMapCache(redisParserVO.getChainKey());
        for (String key : chainKey.keySet()) {
            chainKey.remove(key);
        }
        for (String key : scriptKey.keySet()) {
            scriptKey.remove(key);
        }
        chainKey.keySet().forEach(System.out::println);
        System.out.println("");
        scriptKey.keySet().forEach(System.out::println);
        System.out.println("数据清空完成");
    }*/

    @AfterAll
    public static void cleanScanCache() {
        ComponentScanner.cleanCache();
        FlowBus.cleanCache();
        ExecutorHelper.loadInstance().clearExecutorServiceMap();
        SpiFactoryCleaner.clean();
        LiteflowConfigGetter.clean();
        FlowInitHook.cleanHook();
    }
}
