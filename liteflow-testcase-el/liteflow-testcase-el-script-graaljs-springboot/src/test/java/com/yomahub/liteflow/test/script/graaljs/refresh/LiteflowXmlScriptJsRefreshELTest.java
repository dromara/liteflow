package com.yomahub.liteflow.test.script.graaljs.refresh;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 测试springboot下的graaljs脚本组件，基于xml配置
 *
 * @author zendwang
 * @since 2.9.4
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/refresh/application.properties")
@SpringBootTest(classes = LiteflowXmlScriptJsRefreshELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.graaljs.refresh.cmp" })
public class LiteflowXmlScriptJsRefreshELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试脚本的热重载
	@Test
	public void testRefresh1() throws Exception {
		// 根据配置，加载的应该是flow.xml，执行原来的规则
		LiteflowResponse responseOld = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(responseOld.isSuccess());
		Assertions.assertEquals("d==>s1[选择脚本]==>a", responseOld.getExecuteStepStr());
		// 更改规则，重新加载，更改的规则内容从flow_update.xml里读取，这里只是为了模拟下获取新的内容。不一定是从文件中读取
		String newContent = ResourceUtil.readUtf8Str("classpath: /refresh/flow_update.xml");
		// 进行刷新
		FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML, newContent);

		// 重新执行chain2这个链路，结果会变
		LiteflowResponse responseNew = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(responseNew.isSuccess());
		Assertions.assertEquals("d==>s1[选择脚本_改]==>b==>s2[普通脚本_新增]", responseNew.getExecuteStepStr());
	}

}
