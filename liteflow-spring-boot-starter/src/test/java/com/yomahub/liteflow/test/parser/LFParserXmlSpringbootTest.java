package com.yomahub.liteflow.test.parser;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 切面场景单元测试
 * @author Bryan.Zhang
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/parser/application-xml.properties")
@SpringBootTest(classes = LFParserXmlSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.parser.cmp"})
public class LFParserXmlSpringbootTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试无springboot场景的xml parser
    @Test
    public void testSpringboot() throws Exception{
        LiteflowResponse<Slot> response = flowExecutor.execute("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
