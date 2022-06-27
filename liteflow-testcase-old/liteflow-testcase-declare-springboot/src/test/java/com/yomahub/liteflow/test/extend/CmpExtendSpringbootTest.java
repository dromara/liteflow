package com.yomahub.liteflow.test.extend;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
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
 * springboot环境测试声明式组件继承其他类的场景
 * @author Bryan.Zhang
 * @since 2.7.1
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/extend/application.properties")
@SpringBootTest(classes = CmpExtendSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.extend.cmp"})
public class CmpExtendSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testExtend() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }

}
