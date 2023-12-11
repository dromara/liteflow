package com.yomahub.liteflow.test.declCmp;

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
 * spring环境声明式bean的测试用例
 * 目前方法级声明式在spring环境中不可用
 *
 * @author Bryan.Zhang
 * @since 2.11.4
 */
/*@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/declCmp/application.xml")*/
public class DeclCmpSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	/*@Test
	public void testDeclCmp() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}*/

}
