package com.yomahub.liteflow.test.redis;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.*;
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
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

    @AfterEach
    public void after() {
        FlowBus.cleanCache();
    }

    /**
     * 测试chain
     */
    @Test
    public void testPollWithXml() {
        Set<String> chainNameSet = new HashSet<>();
        chainNameSet.add("chain11");
        String chainValue = "THEN(a, b, c);";
        when(chainJedis.hkeys("pollChainKey")).thenReturn(chainNameSet);
        when(chainJedis.hget("pollChainKey", "chain11")).thenReturn(chainValue);

        LiteflowResponse response = flowExecutor.execute2Resp("chain11", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());
    }

    /**
     * 测试script
     */
    @Test
    public void testPollWithScriptXml() {
        Set<String> chainNameSet = new HashSet<>();
        chainNameSet.add("chain22");
        String chainValue = "THEN(a, b, c, s11, s22, s33);";
        when(chainJedis.hkeys("pollChainKey")).thenReturn(chainNameSet);
        when(chainJedis.hget("pollChainKey", "chain22")).thenReturn(chainValue);

        Set<String> scriptFieldSet = new HashSet<>();
        scriptFieldSet.add("s11:script:脚本s11:groovy");
        scriptFieldSet.add("s22:script:脚本s22:js");
        scriptFieldSet.add("s33:script:脚本s33");
        String s11 = "defaultContext.setData(\"test11\",\"hello s11\");";
        String s22 = "defaultContext.setData(\"test22\",\"hello s22\");";
        String s33 = "defaultContext.setData(\"test33\",\"hello s33\");";

        when(scriptJedis.hkeys("pollScriptKey")).thenReturn(scriptFieldSet);
        when(scriptJedis.hget("pollScriptKey", "s11:script:脚本s11:groovy")).thenReturn(s11);
        when(scriptJedis.hget("pollScriptKey", "s22:script:脚本s22:js")).thenReturn(s22);
        when(scriptJedis.hget("pollScriptKey", "s33:script:脚本s33")).thenReturn(s33);

        LiteflowResponse response = flowExecutor.execute2Resp("chain22", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("hello s11", context.getData("test11"));
        Assertions.assertEquals("a==>b==>c==>s11[脚本s11]==>s22[脚本s22]==>s33[脚本s33]", response.getExecuteStepStrWithoutTime());
    }
}
