package com.yomahub.liteflow.test.config;

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
 * spring环境下 rule-source 参数支持通配符，支持模式匹配
 *
 * @author zendwang
 * @since 2.5.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/config/local-rule-source-pattern-match.xml")
public class LocalRuleSourcePatternMatchELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor executor;

	/**
	 * 匹配的文件 config/springgroup0/flow0.json config/springgroup1/flow0.json
	 */
	@Test
	public void testLocalJsonRuleSourcePatternMatch() {
		LiteflowResponse response0 = executor.execute2Resp("chain1", "arg");
		Assertions.assertEquals("a==>b==>c", response0.getExecuteStepStr());
		LiteflowResponse response1 = executor.execute2Resp("chain3", "arg");
		Assertions.assertEquals("a==>c==>f==>g", response1.getExecuteStepStr());
	}

}
