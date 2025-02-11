package com.yomahub.liteflow.test.stepData;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.entity.CmpStep;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.function.Consumer;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/stepData/application.properties")
@SpringBootTest(classes = StepDataSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.stepData.cmp" })
public class StepDataSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试step data,每个step都不一样的数据
	@Test
	public void testStepData1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		response.getExecuteStepQueue().forEach(
				cmpStep -> Assertions.assertEquals(StrUtil.format("step_{}", cmpStep.getNodeId()), cmpStep.getStepData())
		);
	}

	// 测试step data，即便是2个相同的节点，step data也可以不一样
	@Test
	public void testStepData2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		final Object[] data = {null};
		response.getExecuteStepQueue().forEach(cmpStep -> {
            Assertions.assertNotEquals(data[0], cmpStep.getStepData());
            data[0] = cmpStep.getStepData();
        });
	}
}
