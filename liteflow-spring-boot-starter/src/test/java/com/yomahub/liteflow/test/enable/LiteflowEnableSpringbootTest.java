package com.yomahub.liteflow.test.enable;

import com.yomahub.liteflow.test.BaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * 测试springboot下的enable参数
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/enable/application.properties")
@SpringBootTest(classes = LiteflowEnableSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.enable.cmp"})
public class LiteflowEnableSpringbootTest extends BaseTest {

    @Test
    public void testConfig() {
        System.out.println("成功启动，并且打印");
    }
}
