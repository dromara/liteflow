package com.yomahub.liteflow.test.script.javaxpro.override;

import cn.hutool.core.util.BooleanUtil;
import com.yomahub.liteflow.core.FlowExecutor;
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

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/override/application.properties")
@SpringBootTest(classes = ScriptJavaxProOverrideELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.javaxpro.override.cmp" })
public class ScriptJavaxProOverrideELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //覆盖isAccess
    @Test
    public void testJavaxPro1() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", 3, DefaultContext.class);
        Assertions.assertEquals("a", response.getExecuteStepStr());
    }

    //覆盖isContinueOnError
    @Test
    public void testJavaxPro2() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", null, DefaultContext.class);
        Assertions.assertEquals("s2==>a", response.getExecuteStepStrWithoutTime());
    }

    //设置setIsEnd(true)
    @Test
    public void testJavaxPro3() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", null, DefaultContext.class);
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNull(response.getCause());
        Assertions.assertEquals("a==>s3", response.getExecuteStepStrWithoutTime());
    }

    //覆盖beforeProcess和afterProcess
    @Test
    public void testJavaxPro4() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", null, DefaultContext.class);
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>s4", response.getExecuteStepStrWithoutTime());
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue((Boolean) context.getData("before"));
        Assertions.assertTrue((Boolean) context.getData("after"));
    }

    //覆盖onSuccess
    @Test
    public void testJavaxPro5() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain5", null, DefaultContext.class);
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>s5", response.getExecuteStepStrWithoutTime());
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue((Boolean) context.getData("successFlag"));
    }

    //覆盖onError
    @Test
    public void testJavaxPro6() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain6", null, DefaultContext.class);
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals("a==>s6", response.getExecuteStepStrWithoutTime());
        Assertions.assertNotNull(response.getCause());
        Assertions.assertTrue((Boolean) context.getData("errorFlag"));
        Assertions.assertEquals("test error", context.getData("errorMsg"));
    }

    //覆盖rollback
    @Test
    public void testJavaxPro7() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain7", null, DefaultContext.class);
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals("s7==>b", response.getExecuteStepStrWithoutTime());
        Assertions.assertEquals("b==>s7", response.getRollbackStepStrWithoutTime());
        Assertions.assertTrue((Boolean) context.getData("rollbackFlag"));
    }
}