package com.yomahub.liteflow.test.nacos;

import com.alibaba.nacos.client.config.NacosConfigService;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * springboot环境下的nacos配置源功能测试 nacos存储数据的格式为xml文件
 *
 * @author mll
 * @since 2.9.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/nacos/application-xml.properties")
@SpringBootTest(classes = NacosWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.nacos.cmp" })
public class NacosWithXmlELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@MockBean
	private NacosConfigService nacosConfigService;

	@AfterEach
	public void after() {
		FlowBus.cleanCache();
		FlowBus.clearStat();
	}

	@Test
	public void testNacosWithXml1() throws Exception {
		String flowXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, b, c);</chain></flow>";
		when(nacosConfigService.getConfig(anyString(), anyString(), anyLong())).thenReturn(flowXml);

		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertEquals("a==>b==>c", response.getExecuteStepStrWithoutTime());
	}

	@Test
	public void testNacosWithXml2() throws Exception {
		String flowXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain2\">THEN(a, b, c);</chain></flow>";
		String changedFlowXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain2\">THEN(a, c);</chain></flow>";
		when(nacosConfigService.getConfig(anyString(), anyString(), anyLong())).thenReturn(flowXml)
			.thenReturn(changedFlowXml);

		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertEquals("a==>b==>c", response.getExecuteStepStrWithoutTime());

		FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML, changedFlowXml);

		response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertEquals("a==>c", response.getExecuteStepStrWithoutTime());
	}

}
