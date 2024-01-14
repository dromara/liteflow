package com.yomahub.liteflow.test.apollo;

import com.ctrip.framework.apollo.Config;
import com.google.common.collect.Sets;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.Assertions;
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

import javax.annotation.Resource;
import java.util.Set;

import static org.mockito.Mockito.when;

/**
 * @Description:
 * @Author: zhanghua
 * @Date: 2022/12/3 15:22
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/apollo/application-xml.properties")
@SpringBootTest(classes = ApolloWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.apollo.cmp"})
public class ApolloWithXmlELSpringbootTest {

    @MockBean(name = "chainConfig")
    private Config chainConfig;

    @MockBean(name = "scriptConfig")
    private Config scriptConfig;

    @Resource
    private FlowExecutor flowExecutor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testApolloWithXml1() {
        Set<String> chainNameList = Sets.newHashSet("chain1", "chain2:false");
        Set<String> scriptNodeValueList = Sets.newHashSet("s1:script:脚本s1", "s2:script:脚本s1:groovy:false");
        when(chainConfig.getPropertyNames()).thenReturn(chainNameList);
        when(scriptConfig.getPropertyNames()).thenReturn(scriptNodeValueList);

        when(chainConfig.getProperty("chain1", "")).thenReturn("THEN(a, b, c, s1);");
        when(chainConfig.getProperty("chain2:false", "")).thenReturn("THEN(a, b, c, s1);");
        when(scriptConfig.getProperty("s1:script:脚本s1", "")).thenReturn("defaultContext.setData(\"test\",\"hello\");");
        when(scriptConfig.getProperty("s2:script:脚本s1:groovy:false", "")).thenReturn("defaultContext.setData(\"test\",\"hello\");");

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertEquals("a==>b==>c==>s1[脚本s1]", response.getExecuteStepStrWithoutTime());

        // 测试 chain 停用
        Assertions.assertThrows(ChainNotFoundException.class, () -> {
            throw flowExecutor.execute2Resp("chain2", "arg").getCause();
        });

        // 测试 script 停用
        Assertions.assertTrue(!FlowBus.getNodeMap().containsKey("s2"));
    }
}
