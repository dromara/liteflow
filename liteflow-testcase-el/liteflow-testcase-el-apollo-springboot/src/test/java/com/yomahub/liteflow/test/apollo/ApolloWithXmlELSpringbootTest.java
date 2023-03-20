package com.yomahub.liteflow.test.apollo;

import cn.hutool.core.util.StrUtil;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * @Description:
 * @Author: zhanghua
 * @Date: 2022/12/3 15:22
 */
@RunWith(SpringRunner.class)
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

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@After
	public void after() {
		FlowBus.cleanCache();
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
		Assert.assertEquals("a==>b==>c==>s1[脚本s1]", response.getExecuteStepStrWithoutTime());
	}

}
