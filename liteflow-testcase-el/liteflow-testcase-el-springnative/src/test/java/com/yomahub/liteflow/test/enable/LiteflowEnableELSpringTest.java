package com.yomahub.liteflow.test.enable;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

/**
 * spring环境下enable参数
 *
 * @author qjwyss
 * @since 2.6.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/enable/application-local.xml")
public class LiteflowEnableELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testEnable() throws Exception {
		LiteflowConfig config = LiteflowConfigGetter.get();
		Boolean enable = config.getEnable();
		if (enable) {
			LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
			Assertions.assertTrue(response.isSuccess());
			return;
		}

		Assertions.assertFalse(enable);
	}

}
