package com.yomahub.liteflow.test.parser;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.test.BaseTest;
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
 * spring环境的json parser单元测试
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/parser/application-json.properties")
@SpringBootTest(classes = LFParserJsonSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.parser.cmp"})
public class LFParserJsonSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试spring场景的json parser
    @Test
    public void testSpringboot() throws Exception{
        LiteflowResponse<Slot> response = flowExecutor.execute("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
