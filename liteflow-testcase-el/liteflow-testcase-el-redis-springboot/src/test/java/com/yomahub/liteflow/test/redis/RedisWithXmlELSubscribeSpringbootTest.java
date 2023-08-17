package com.yomahub.liteflow.test.redis;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.redis.mode.RClient;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
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

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

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
public class RedisWithXmlELSubscribeSpringbootTest extends BaseTest {

    @MockBean(name = "chainClient")
    private static RClient chainClient;

    @MockBean(name = "scriptClient")
    private static RClient scriptClient;

    @Resource
    private FlowExecutor flowExecutor;

    @AfterEach
    public void after() {
        FlowBus.cleanCache();
    }

    /**
     * 测试 chain
     */
    @Test
    public void testSubWithXml() {
        Map<String, String> chainMap = new HashMap<>();
        chainMap.put("chain1", "THEN(a, b, c);");
        //修改chain值
        Map<String, String> changeChainMap = new HashMap<>();
        changeChainMap.put("chain1", "THEN(a, c);");
        when(chainClient.getMap("chainKey")).thenReturn(chainMap).thenReturn(changeChainMap);

        //测试修改前的chain
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());

        flowExecutor.reloadRule();

        //测试修改后的chain
        response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>c", response.getExecuteStepStr());
    }

    @Test
    public void testSubWithScriptXml() {
        Map<String, String> chainMap = new HashMap<>();
        chainMap.put("chain2", "THEN(a, b, c, s1, s2, s3);");

        Map<String, String> scriptMap = new HashMap<>();
        scriptMap.put("s1:script:脚本s1:groovy", "defaultContext.setData(\"test1\",\"hello s1\");");
        scriptMap.put("s2:script:脚本s2:js", "defaultContext.setData(\"test2\",\"hello s2\");");
        scriptMap.put("s3:script:脚本s3", "defaultContext.setData(\"test3\",\"hello s3\");");
        //修改chain值和script值
        Map<String, String> changeChainMap = new HashMap<>();
        changeChainMap.put("chain2", "THEN(a, c, s1, s3);");
        Map<String, String> changeScriptMap = new HashMap<>();
        changeScriptMap.put("s1:script:脚本s1:groovy", "defaultContext.setData(\"test1\",\"hello world\");");
        changeScriptMap.put("s2:script:脚本s2:js", "defaultContext.setData(\"test2\",\"hello s2\");");
        changeScriptMap.put("s3:script:脚本s3", "defaultContext.setData(\"test3\",\"hello s3\");");

        when(chainClient.getMap("chainKey")).thenReturn(chainMap).thenReturn(changeChainMap);
        //这里是因为脚本的getMap方法在一次流程里会执行到两次
        when(scriptClient.getMap("scriptKey")).thenReturn(scriptMap).thenReturn(scriptMap)
                .thenReturn(changeScriptMap).thenReturn(changeScriptMap);

        //测试修改前的chain和script
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("hello s1", context.getData("test1"));
        Assertions.assertEquals("hello s2", context.getData("test2"));
        Assertions.assertEquals("a==>b==>c==>s1[脚本s1]==>s2[脚本s2]==>s3[脚本s3]", response.getExecuteStepStrWithoutTime());

        flowExecutor.reloadRule();

        //测试修改后的chain和script
        response = flowExecutor.execute2Resp("chain2", "arg");
        context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("hello world", context.getData("test1"));
        Assertions.assertEquals("a==>c==>s1[脚本s1]==>s3[脚本s3]", response.getExecuteStepStrWithoutTime());
    }
}
