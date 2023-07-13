package com.yomahub.liteflow.test.execute2Future;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import javax.annotation.Resource;
import java.util.concurrent.Future;

/**
 * spring环境执行返回future的例子
 *
 * @author Bryan.Zhang
 * @since 2.6.13
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/execute2Future/application.xml")
public class Executor2FutureELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testFuture() throws Exception {
		Future<LiteflowResponse> future = flowExecutor.execute2Future("chain1", "arg", DefaultContext.class);
		LiteflowResponse response = future.get();
		Assertions.assertTrue(response.isSuccess());
	}

}
