package com.yomahub.liteflow.test.instanceIds;

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
/**
 * 测试生成 instanceId
 * @author Jay li
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/instanceIds/application.xml")
public class InstanceIdELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

    // 文件保存实例id
	@Test
	public void testInstanceIds1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>a==>a==>a", response.getExecuteStepStr());

		String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
		Set<String> strings = extractValues(executeStepStrWithInstanceId);
		System.out.println(executeStepStrWithInstanceId);
		Assertions.assertEquals(strings.size(), 4);
	}

	// 重复调用实例id不变
	@Test
	public void testInstanceIds2() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>a==>a==>a", response.getExecuteStepStr());

		String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
		Set<String> set1 = extractValues(executeStepStrWithInstanceId);
		System.out.println(executeStepStrWithInstanceId);

		Assertions.assertEquals(set1.size(), 4);

		response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>a==>a==>a", response.getExecuteStepStr());

		executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
		Set<String> set2 = extractValues(executeStepStrWithInstanceId);
		System.out.println(executeStepStrWithInstanceId);

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
