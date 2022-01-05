package com.yomahub.liteflow.test.component;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.exception.ChainEndException;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * 组件功能点测试
 * 单元测试
 *
 * @author donguo.tao
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/component/application.properties")
@SpringBootTest(classes = FlowExecutorTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.component.cmp1","com.yomahub.liteflow.test.component.cmp2"})
public class FlowExecutorTest extends BaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(FlowExecutorTest.class);

    @Resource
    private FlowExecutor flowExecutor;

    //isAccess方法的功能测试
    @Test
    public void testIsAccess() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", 101);
        Assert.assertTrue(response.isSuccess());
        Assert.assertNotNull(response.getSlot().getResponseData());
    }

    //组件抛错的功能点测试
    @Test(expected = ArithmeticException.class)
    public void testComponentException() throws Exception {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain2", 0);
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals("/ by zero", response.getMessage());
        ReflectionUtils.rethrowException(response.getCause());
    }

    //isContinueOnError方法的功能点测试
    @Test(expected = UndeclaredThrowableException.class)
    public void testIsContinueOnError() throws Exception {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain3", 0);
        Assert.assertTrue(response.isSuccess());
        ReflectionUtils.rethrowException(response.getCause());
    }

    //isEnd方法的功能点测试
    @Test
    public void testIsEnd() throws Exception {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain4", 10);
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("d",response.getSlot().getExecuteStepStr());
    }

    //setIsEnd方法的功能点测试
    @Test
    public void testSetIsEnd1() throws Exception {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain5", 10);
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("e",response.getSlot().getExecuteStepStr());
    }

    //条件组件的功能点测试
    @Test
    public void testNodeCondComponent() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain6", 0);
        Assert.assertTrue(response.isSuccess());
    }

    //测试setIsEnd如果为true，continueError也为true，那不应该continue了
    @Test
    public void testSetIsEnd2() throws Exception {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain7", 10);
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("g",response.getSlot().getExecuteStepStr());
    }

}
