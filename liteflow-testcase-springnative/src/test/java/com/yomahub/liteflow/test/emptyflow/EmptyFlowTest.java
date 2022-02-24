package com.yomahub.liteflow.test.emptyflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 切面场景单元测试
 * @author Bryan.Zhang
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/emptyFlow/application.xml")
public class EmptyFlowTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试空flow的情况下，liteflow是否能正常启动
    @Test
    public void testEmptyFlow() {
        //不做任何事，为的是能正常启动
    }
}
