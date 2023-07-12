package com.yomahub.liteflow.test.reload;

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
 * spring环境下重新加载规则测试
 *
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/reload/application.xml")
public class ReloadELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 用reloadRule去重新加载，这里如果配置是放在本地。如果想修改，则要去修改target下面的flow.xml
	// 这里的测试，手动打断点然后去修改，是ok的。但是整个测试，暂且只是为了测试这个功能是否能正常运行
	@Test
	public void testReload() throws Exception {
		flowExecutor.reloadRule();
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
