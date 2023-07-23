package com.yomahub.liteflow.test.customNodes;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import javax.annotation.Resource;

/**
 * spring环境下自定义声明节点的测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/customNodes/application.xml")
public class CustomNodesELSpringTest extends BaseTest {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testCustomNodes() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
