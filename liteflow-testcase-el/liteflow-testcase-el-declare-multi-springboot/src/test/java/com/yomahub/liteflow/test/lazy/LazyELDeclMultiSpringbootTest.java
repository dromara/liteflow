package com.yomahub.liteflow.test.lazy;

import com.yomahub.liteflow.test.BaseTest;

//spring的延迟加载在el表达形式模式下不起作用
/*@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/lazy/application.properties")
@SpringBootTest(classes = LazyELDeclSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.lazy.cmp"})*/
public class LazyELDeclMultiSpringbootTest extends BaseTest {

	/*
	 * @Resource private FlowExecutor flowExecutor;
	 *
	 * @Test public void testLazy() throws Exception{ LiteflowResponse response =
	 * flowExecutor.execute2Resp("chain1", "arg");
	 * Assertions.assertTrue(response.isSuccess()); }
	 */

}
