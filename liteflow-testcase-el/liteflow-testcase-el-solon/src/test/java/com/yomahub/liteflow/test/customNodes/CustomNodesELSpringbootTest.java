package com.yomahub.liteflow.test.customNodes;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * springboot环境下自定义声明节点的测试
 * 不通过spring扫描的方式，通过在配置文件里定义nodes的方式
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/customNodes/application.properties")
public class CustomNodesELSpringbootTest extends BaseTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private FlowExecutor flowExecutor;

    @Test
    public void testCustomNodes() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
