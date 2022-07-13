package com.yomahub.liteflow.test.switchError;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
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
 * springboot环境EL常规的例子测试
 * @author Bryan.Zhang
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/switchError/application.properties")
@SpringBootTest(classes = SwitchELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.switchError.cmp"})
public class SwitchELSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //2022-07-12 switch 异常错误.c.y.l.builder.el.operator.ToOperator     : parameter error
    //run QlExpress Exception at line 1 :
    // switch().to(): 只有一个node时出错
    @Test
    public void testBase1() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("testBase1", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
