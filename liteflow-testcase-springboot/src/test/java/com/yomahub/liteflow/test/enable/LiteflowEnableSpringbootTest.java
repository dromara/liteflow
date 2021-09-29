package com.yomahub.liteflow.test.enable;

import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * 测试springboot下的enable参数
 *
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/enable/application.properties")
@SpringBootTest(classes = LiteflowEnableSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.enable.cmp"})
public class LiteflowEnableSpringbootTest extends BaseTest {

    @Autowired
    private ApplicationContext context;


    @Test
    public void testEnable() {
        LiteflowConfig config = context.getBean(LiteflowConfig.class);
        Boolean enable = config.getEnable();
        if (enable) {
            System.out.println("成功启动，并且打印");
            return;
        }
        Assert.assertFalse(enable);
    }
}
