package com.yomahub.liteflow.test.maxWaitSeconds;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.WhenTimeoutException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.concurrent.TimeoutException;

import static com.yomahub.liteflow.test.maxWaitSeconds.cmp.CmpConfig.CONTENT_KEY;

/**
 * Spring Boot 环境下超时控制测试
 *
 * @author DaleLee
 * @since 2.11.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/maxWaitSeconds/application.properties")
@SpringBootTest(classes = MaxWaitSecondsELDeclMultiSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.maxWaitSeconds.cmp"})
public class MaxWaitSecondsELDeclMultiSpringbootTest extends BaseTest {
    @Resource
    private FlowExecutor flowExecutor;

    // 测试 THEN 的超时情况
    @Test
    public void testThen1() {
        assertTimeout("then1");
    }

    // 测试 THEN 的非超时情况
    @Test
    public void testThen2() {
        assertNotTimeout("then2");
    }

    // 测试 When 的超时情况
    @Test
    public void testWhen1() {
        assertWhenTimeout("when1");
    }

    // 测试 WHEN 的非超时情况
    @Test
    public void testWhen2() {
        assertNotTimeout("when2");
    }

    // 测试 FOR 的超时情况
    @Test
    public void testFor1() {
        assertTimeout("for1");
    }

    // 测试 FOR 的非超时情况
    @Test
    public void testFor2() {
        assertNotTimeout("for2");
    }

    // 测试 WHILE 的超时情况
    @Test
    public void testWhile1() {
        assertTimeout("while1");
    }

    // 测试 WHILE 的非超时情况
    @Test
    public void testWhile2() {
        assertNotTimeout("while2");
    }

    // 测试 ITERATOR 的超时情况
    @Test
    public void testIterator1() {
        assertTimeout("iterator1");
    }

    // 测试 ITERATOR 的非超时情况
    @Test
    public void testIterator2() {
        assertNotTimeout("iterator2");
    }

    // 测试 SWITCH 的超时情况
    @Test
    public void testSwitch1() {
        assertTimeout("switch1");
    }

    // 测试 SWITCH 的非超时情况
    @Test
    public void testSwitch2() {
        assertNotTimeout("switch2");
    }

    // 测试 IF 的超时情况
    @Test
    public void testIf1() {
        assertTimeout("if1");
    }

    // 测试 SWITCH 的非超时情况
    @Test
    public void testIf2() {
        assertNotTimeout("if2");
    }

    // 测试单个组件的超时情况
    @Test
    public void testComponent1() {
        assertTimeout("component1");
    }

    // 测试单个组件的非超时情况
    @Test
    public void testComponent2() {
        assertNotTimeout("component2");
    }

    // 测试 FINALLY，虽然超时，但 FINALLY 仍会执行
    @Test
    public void testFinally1() {
        LiteflowResponse response = flowExecutor.execute2Resp("finally", "arg");
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(TimeoutException.class, response.getCause().getClass());
        // FINALLY 执行时在默认数据上下文中放入了 CONTENT_KEY
        DefaultContext contextBean = response.getFirstContextBean();
        Assertions.assertTrue(contextBean.hasData(CONTENT_KEY));
    }

    // 测试 maxWaitSeconds 关键字不能作用于 Finally
    @Test
    public void testFinally2() {
        Assertions.assertFalse(LiteFlowChainELBuilder.validate("THEN(a, b, FINALLY(c).maxWaitSeconds(10))"));
    }

    // 测试 chain 的超时情况
    @Test
    public void testChain1() {
        assertTimeout("chain1");
    }

    // 测试 chain 的非超时情况
    @Test
    public void testChain2() {
        assertNotTimeout("chain2");
    }

    private void assertTimeout(String chainId) {
        LiteflowResponse response = flowExecutor.execute2Resp(chainId, "arg");
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(TimeoutException.class, response.getCause().getClass());
    }

    private void assertWhenTimeout(String chainId) {
        LiteflowResponse response = flowExecutor.execute2Resp(chainId, "arg");
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(WhenTimeoutException.class, response.getCause().getClass());
    }

    private void assertNotTimeout(String chainId) {
        LiteflowResponse response = flowExecutor.execute2Resp(chainId, "arg");
        Assertions.assertTrue(response.isSuccess());
    }
}
