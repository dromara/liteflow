package com.yomahub.liteflow.test.parsecustom;

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
 * springboot环境的自定义json parser单元测试
 * @since 2.5.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/parsecustom/application.properties")
@SpringBootTest(classes = CustomParserJsonSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.parsecustom.cmp"})
public class CustomParserJsonSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试springboot场景的自定义json parser
    @Test
    public void testSpringboot() throws Exception{
        LiteflowResponse<Slot> response = flowExecutor.execute("chain1", "args");
        Assert.assertTrue(response.isSuccess());
    }
}
