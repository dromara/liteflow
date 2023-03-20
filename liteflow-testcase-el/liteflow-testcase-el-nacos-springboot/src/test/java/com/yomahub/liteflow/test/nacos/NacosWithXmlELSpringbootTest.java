package com.yomahub.liteflow.test.nacos;

import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.client.config.impl.LocalConfigInfoProcessor;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.nacos.util.NacosParserHelper;
import com.yomahub.liteflow.parser.nacos.vo.NacosParserVO;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * springboot环境下的nacos配置源功能测试 nacos存储数据的格式为xml文件
 *
 * @author mll
 * @since 2.9.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/nacos/application-xml.properties")
@SpringBootTest(classes = NacosWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.nacos.cmp" })
public class NacosWithXmlELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@MockBean
	private NacosConfigService nacosConfigService;

	@After
	public void after() {
		FlowBus.cleanCache();
	}

	@Test
	public void testNacosWithXml1() throws Exception {
		String flowXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, b, c);</chain></flow>";
		when(nacosConfigService.getConfig(anyString(), anyString(), anyLong())).thenReturn(flowXml);

		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertEquals("a==>b==>c", response.getExecuteStepStrWithoutTime());
	}

	@Test
	public void testNacosWithXml2() throws Exception {
		String flowXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, b, c);</chain></flow>";
		String changedFlowXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, c);</chain></flow>";
		when(nacosConfigService.getConfig(anyString(), anyString(), anyLong())).thenReturn(flowXml)
			.thenReturn(changedFlowXml);

		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertEquals("a==>b==>c", response.getExecuteStepStrWithoutTime());

		FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML, changedFlowXml);

		response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertEquals("a==>c", response.getExecuteStepStrWithoutTime());
	}

}
