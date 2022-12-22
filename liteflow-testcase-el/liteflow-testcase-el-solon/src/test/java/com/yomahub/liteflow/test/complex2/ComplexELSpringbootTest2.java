package com.yomahub.liteflow.test.complex2;

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
 * springboot环境EL复杂例子测试1
 * @author Bryan.Zhang
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/complex/application2.properties")
public class ComplexELSpringbootTest2 extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    //测试复杂例子，优化前
    //案例来自于文档中 EL规则写法/复杂编排例子/复杂例子二
    //因为所有的组件都是空执行，你可以在组件里加上Thread.sleep来模拟业务耗时，再来看这个打出结果
    @Test
    public void testComplex2_1() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain2_1", "arg");
        Assert.assertTrue(response.isSuccess());
    }

    //测试复杂例子，优化后
    //案例来自于文档中 EL规则写法/复杂编排例子/复杂例子二
    //因为所有的组件都是空执行，你可以在组件里加上Thread.sleep来模拟业务耗时，再来看这个打出结果
    @Test
    public void testComplex2_2() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain2_2", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
