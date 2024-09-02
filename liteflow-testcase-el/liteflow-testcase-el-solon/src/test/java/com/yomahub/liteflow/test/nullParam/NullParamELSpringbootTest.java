package com.yomahub.liteflow.test.nullParam;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * 单元测试:传递null param导致NPE的优化代码
 *
 * @author LeoLee
 * @since 2.6.6
 */
@SolonTest
@Import(profiles="classpath:/nullParam/application.properties")
public class NullParamELSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	/**
	 * 支持无参的flow执行，以及param 为null时的异常抛出
	 */
	@Test
	public void testNullParam() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1");
		Assertions.assertTrue(response.isSuccess());
	}

}
