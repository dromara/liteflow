package com.yomahub.liteflow.test.scenes;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.scenes.bean.RequestDataDO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * springboot环境EL复杂例子测试1
 * @author nmnl
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/scens/application1.properties")
@SpringBootTest(classes = ScensELSpringbootTest1.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.scenes.cmp1"})
public class ScensELSpringbootTest1 extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;


    // 验证正常finally
    /*@Test
    public void testScens1_1() throws Exception{
        LocalDateTime x = LocalDateTime.of(LocalDate.now(), LocalTime.of(15,0,0));
        LiteflowResponse response = flowExecutor.execute2Resp("chaintask", RequestDataDO.of(x,"a"));
        //Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("ASwitchCmp==>FinallyCmp",response.getExecuteStepStr());
    }*/
}
