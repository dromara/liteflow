package com.yomahub.liteflow.test.validateRule;

import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.validateRule.cmp.ACmp;
import com.yomahub.liteflow.test.validateRule.cmp.BCmp;
import com.yomahub.liteflow.test.validateRule.cmp.CCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ValidateRuleELSpringbootTest.class)
@EnableAutoConfiguration
public class ValidateRuleELSpringbootTest extends BaseTest {

	@Test
	public void testChainELExpressValidate() {
		LiteFlowNodeBuilder.createNode()
			.setId("a")
			.setName("组件A")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(ACmp.class)
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("b")
			.setName("组件B")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(BCmp.class)
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("c")
			.setName("组件C")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(CCmp.class)
			.build();
		Assertions.assertFalse(LiteFlowChainELBuilder.validate("THEN(a, b, h)"));
		Assertions.assertTrue(LiteFlowChainELBuilder.validate("THEN(a, b, c)"));
	}

}
