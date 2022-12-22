package com.yomahub.liteflow.test.reload;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

/**
 * springboot环境下重新加载规则测试
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/reload/application.properties")
public class ReloadELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    //用reloadRule去重新加载，这里如果配置是放在本地。如果想修改，则要去修改target下面的flow.xml
    //这里的测试，手动打断点然后去修改，是ok的。但是整个测试，暂且只是为了测试这个功能是否能正常运行
    @Test
    public void testReload() throws Exception{
        flowExecutor.reloadRule();
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
