package com.yomahub.liteflow.test.parser;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * 无spring环境的xml parser单元测试
 * @author Bryan.Zhang
 * @since 2.5.0
 */
public class LFParserXmlNoSpringTest extends BaseTest {

    //测试无spring场景的xml parser
    @Test
    public void testNoSpring() {
        LiteflowConfig liteflowConfig = new LiteflowConfig();
        liteflowConfig.setRuleSource("parser/flow.xml");
        FlowExecutor executor = new FlowExecutor(liteflowConfig);
        LiteflowResponse<DefaultSlot> response = executor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
