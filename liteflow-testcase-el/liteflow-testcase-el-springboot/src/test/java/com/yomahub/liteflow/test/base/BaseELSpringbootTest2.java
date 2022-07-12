package com.yomahub.liteflow.test.base;

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
@TestPropertySource(value = "classpath:/base/application.properties")
@SpringBootTest(classes = BaseELSpringbootTest2.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.base.cmp"})
public class BaseELSpringbootTest2 extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //最简单的情况
    //2022-07-12 SWITCH 异常错误.[com.yomahub.liteflow.builder.el.operator.ToOperator.executeInner(45)] : The parameter must be Executable item!
    @Test
    public void testBase1() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
