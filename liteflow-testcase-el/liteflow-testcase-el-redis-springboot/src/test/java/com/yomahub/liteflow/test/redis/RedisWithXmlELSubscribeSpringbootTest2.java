package com.yomahub.liteflow.test.redis;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * springboot环境下的redis配置源订阅模式功能测试
 *
 * @author hxinyu
 * @since 2.11.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/redis/application-sub-xml.properties")
@SpringBootTest(classes = RedisWithXmlELSubscribeSpringbootTest2.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.redis.cmp"})
public class RedisWithXmlELSubscribeSpringbootTest2 extends BaseTest {

    @MockBean(name = "chainClient")
    private static RedissonClient chainClient;

    @MockBean(name = "scriptClient")
    private static RedissonClient scriptClient;

    @Resource
    private FlowExecutor flowExecutor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

/*    @BeforeAll
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
    }*/

    @Test
    public void testSubWithXml() throws InterruptedException {
        RMapCache<Object, Object> chainKey = chainClient.getMapCache("");
        System.out.println(chainKey);

/*        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());*/
    }

    @Test
    public void testSubWithScriptXml() throws InterruptedException {
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("hello s1", context.getData("test1"));
        Assertions.assertEquals("a==>b==>c==>s1[脚本s1]==>s2[脚本s2]", response.getExecuteStepStrWithoutTime());
    }
}
