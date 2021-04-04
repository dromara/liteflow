package com.yomahub.liteflow.test.parser;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/parser/application-xml.xml")
public class LFParserXmlSpringTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试spring场景的xml parser
    @Test
    public void testSpring() throws Exception{
        LiteflowResponse<Slot> response = flowExecutor.execute("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
