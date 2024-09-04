package com.yomahub.liteflow.test.cmpData;

import cn.hutool.core.date.DateUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.cmpData.vo.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * solon环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@SolonTest
@Import(profiles="classpath:/cmpData/application.properties")
public class CmpDataELSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	// 最简单的情况
	@Test
	public void testCmpData() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		User user = context.getData("user");
		Assertions.assertEquals(27, user.getAge());
		Assertions.assertEquals("jack", user.getName());
		Assertions.assertEquals(0, user.getBirth().compareTo(DateUtil.parseDate("1995-10-01").toJdkDate()));
	}

}
