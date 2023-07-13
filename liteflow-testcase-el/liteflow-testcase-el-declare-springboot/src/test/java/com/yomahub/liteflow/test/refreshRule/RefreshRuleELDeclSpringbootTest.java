package com.yomahub.liteflow.test.refreshRule;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境下重新加载规则测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/refreshRule/application.properties")
@SpringBootTest(classes = RefreshRuleELDeclSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.refreshRule.cmp" })
public class RefreshRuleELDeclSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试普通刷新流程的场景
	@Test
	public void testRefresh1() throws Exception {
		String content = ResourceUtil.readUtf8Str("classpath: /refreshRule/flow_update.el.xml");
		FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML, content);
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// 测试优雅刷新的场景
	@Test
	public void testRefresh2() throws Exception {
		new Thread(() -> {
			try {
				Thread.sleep(2000L);
				String content = ResourceUtil.readUtf8Str("classpath: /refreshRule/flow_update.el.xml");
				FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML, content);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

		}).start();

		for (int i = 0; i < 500; i++) {
			LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
			Assertions.assertTrue(response.isSuccess());
			try {
				Thread.sleep(10L);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
