package com.yomahub.liteflow.test.parsecustom;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

/**
 * spring环境的自定义json parser单元测试
 *
 * @author dongguo.tao
 * @since 2.5.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/parsecustom/application.xml")
public class CustomParserJsonELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试spring场景的自定义json parser
	@Test
	public void testJsonCustomParser() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "args");
		Assertions.assertTrue(response.isSuccess());
	}

}
