package com.yomahub.liteflow.test.emptyflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.aop.aspect.CustomAspect;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 切面场景单元测试
 * @author Bryan.Zhang
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/emptyFlow/application.properties")
@SpringBootTest(classes = EmptyFlowTest.class)
@EnableAutoConfiguration
@Import(CustomAspect.class)
public class EmptyFlowTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试空flow的情况下，liteflow是否能正常启动
    @Test
    public void testEmptyFlow() {
        //不做任何事，为的是能正常启动
    }
}
