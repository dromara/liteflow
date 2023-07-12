package com.yomahub.liteflow.test.iterator;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/iterator/application.xml")
public class IteratorELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 最简单的情况
	@Test
	public void testIt1() throws Exception {
		List<String> list = ListUtil.toList("1", "2", "3");
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", list);
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		String str = context.getData("test");
		Assertions.assertEquals("123", str);
	}

	// 迭代器带break
	@Test
	public void testIt2() throws Exception {
		List<String> list = ListUtil.toList("1", "2", "3");
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", list);
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		String str = context.getData("test");
		Assertions.assertEquals("12", str);
	}

}
