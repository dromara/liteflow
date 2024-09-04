package com.yomahub.liteflow.test.lazy;

import com.yomahub.liteflow.test.BaseTest;

//spring的延迟加载在el表达形式模式下不起作用
/*
@Import(profiles ="classpath:/lazy/application.properties")
@SolonTest
*/
public class LazyELDeclMultiSpringbootTest extends BaseTest {

	/*
	 * @Inject private FlowExecutor flowExecutor;
	 *
	 * @Test public void testLazy() throws Exception{ LiteflowResponse response =
	 * flowExecutor.execute2Resp("chain1", "arg");
	 * Assertions.assertTrue(response.isSuccess()); }
	 */

}
