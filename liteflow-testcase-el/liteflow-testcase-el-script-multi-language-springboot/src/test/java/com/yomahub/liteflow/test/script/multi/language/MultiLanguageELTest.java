package com.yomahub.liteflow.test.script.multi.language;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 测试springboot下的groovy脚本组件，基于xml配置
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/multiLanguage/application.properties")
@SpringBootTest(classes = MultiLanguageELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.multi.language.cmp" })
public class MultiLanguageELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试普通脚本节点
	@Test
	public void testMultiLanguage1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Object student = context.getData("student");
		Map<String, Object> studentMap = JsonUtil.parseObject(JsonUtil.toJsonString(student), Map.class);
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals(Integer.valueOf(18), context.getData("s1"));
		Assertions.assertEquals(10032, studentMap.get("studentID"));
	}

}
