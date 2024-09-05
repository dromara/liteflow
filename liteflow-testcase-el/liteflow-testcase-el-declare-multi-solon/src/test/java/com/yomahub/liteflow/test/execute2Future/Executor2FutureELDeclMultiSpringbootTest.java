package com.yomahub.liteflow.test.execute2Future;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;
import java.util.concurrent.Future;

/**
 * springboot环境执行返回future的例子
 *
 * @author Bryan.Zhang
 * @since 2.6.13
 */
@Import(profiles ="classpath:/execute2Future/application.properties")
@SolonTest
public class Executor2FutureELDeclMultiSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	@Test
	public void testFuture() throws Exception {
		Future<LiteflowResponse> future = flowExecutor.execute2Future("chain1", "arg", DefaultContext.class);
		LiteflowResponse response = future.get();
		Assertions.assertTrue(response.isSuccess());
	}

}
