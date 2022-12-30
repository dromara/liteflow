package com.yomahub.liteflow.test;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * springboot环境下的nacos配置源功能测试
 * nacos存储数据的格式为xml文件
 * @author mll
 * @since 2.9.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/markdown/application-xml.properties")
@SpringBootTest(classes = MarkdownWithLocalMDSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.cmp"})
public class MarkdownWithLocalMDSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @After
    public void after(){
        FlowBus.cleanCache();
    }

    @Test
    public void testForCase3() throws Exception {
        LiteflowResponse response = flowExecutor.execute2Resp("测试编排003", "arg");
        Assert.assertEquals("A==>B==>C==>D", response.getExecuteStepStrWithoutTime());
    }

}
