package com.yomahub.liteflow.test.apollo;

import com.ctrip.framework.apollo.Config;
import com.google.common.collect.Sets;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
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
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import javax.annotation.Resource;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * @Description:
 * @Author: zhanghua
 * @Date: 2022/12/3 15:22
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/apollo/application-xml.properties")
@SpringBootTest(classes = ApolloWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.apollo.cmp" })
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
		Set<String> chainNameList = Sets.newHashSet("chain1");
		Set<String> scriptNodeValueList = Sets.newHashSet("s1:script:脚本s1");
		when(chainConfig.getPropertyNames()).thenReturn(chainNameList);
		when(scriptConfig.getPropertyNames()).thenReturn(scriptNodeValueList);

		String chain1Data = "THEN(a, b, c, s1);";
		String scriptNodeValue = "defaultContext.setData(\"test\",\"hello\");";
		when(chainConfig.getProperty(anyString(), anyString())).thenReturn(chain1Data);
		when(scriptConfig.getProperty(anyString(), anyString())).thenReturn(scriptNodeValue);

		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertEquals("a==>b==>c==>s1[脚本s1]", response.getExecuteStepStrWithoutTime());
	}
}
