package com.yomahub.liteflow.test.nullParam;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 单元测试:传递null param导致NPE的优化代码
 *
 * @author LeoLee
 * @since 2.6.6
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/nullParam/application.properties")
@SpringBootTest(classes = NullParamSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.nullParam.cmp"})
public class NullParamSpringbootTest {

    @Autowired
    private FlowExecutor flowExecutor;

    /**
     * 支持无参的flow执行，以及param 为null时的异常抛出
     */
    @Test
    public void testNullParam() throws Exception {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1");
        Assert.assertTrue(response.isSuccess());
    }

}
