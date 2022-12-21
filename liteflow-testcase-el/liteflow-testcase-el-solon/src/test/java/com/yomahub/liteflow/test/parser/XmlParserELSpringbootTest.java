package com.yomahub.liteflow.test.parser;

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
 * springboot环境的xml parser单元测试
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/parser/application-xml.properties")
public class XmlParserELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    //测试无springboot场景的xml parser
    @Test
    public void testXmlParser() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
