package com.yomahub.liteflow.test.base;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/base/application.xml")
public class BaseCommonELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testBaseCommon() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c==>d", response.getExecuteStepStr());
	}

	@Test
	public void testBaseCommon2() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>a==>a==>a", response.getExecuteStepStr());

		String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
		Set<String> strings = extractValues(executeStepStrWithInstanceId);

		Assertions.assertEquals(strings.size(), 4);
	}

	@Test
	public void testBaseCommonInstanceId() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>a==>a==>a", response.getExecuteStepStr());

		String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
		Set<String> set1 = extractValues(executeStepStrWithInstanceId);

		Assertions.assertEquals(set1.size(), 4);

		response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>a==>a==>a", response.getExecuteStepStr());

		executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
		Set<String> set2 = extractValues(executeStepStrWithInstanceId);

		Assertions.assertEquals(set2.size(), 4);
		Assertions.assertEquals(set1, set2);
	}

	public static Set<String> extractValues(String input) {
		Set<String> values = new HashSet<>();
		Pattern pattern = Pattern.compile("\\[(.*?)]");
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()) {
			values.add(matcher.group(1));
		}
		return values;
	}

}
