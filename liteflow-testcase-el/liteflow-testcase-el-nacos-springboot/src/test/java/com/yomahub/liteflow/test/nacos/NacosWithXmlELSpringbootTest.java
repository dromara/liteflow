package com.yomahub.liteflow.test.nacos;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
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
 * springboot环境下的nacos配置源功能测试
 * nacos存储数据的格式为xml文件
 * @author mll
 * @since 2.9.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/nacos/application-xml.properties")
@SpringBootTest(classes = NacosWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.nacos.cmp"})
public class NacosWithXmlELSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testNacosWithXml() throws Exception {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c", response.getExecuteStepStr());
        for (int i = 0 ; i < 10; i ++){
            Thread.sleep(10000L);
            response = flowExecutor.execute2Resp("chain1", "arg");
            System.out.println(" i " + response.getExecuteStepStr());
        }
    }
}
