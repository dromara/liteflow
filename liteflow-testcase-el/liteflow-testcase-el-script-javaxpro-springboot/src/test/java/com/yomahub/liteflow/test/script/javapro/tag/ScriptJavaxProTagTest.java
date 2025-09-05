package com.yomahub.liteflow.test.script.javapro.tag;

import cn.hutool.core.collection.ConcurrentHashSet;
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
@TestPropertySource(value = "classpath:/tag/application.properties")
@SpringBootTest(classes = ScriptJavaxProTagTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.javapro.tag.aspect" })
public class ScriptJavaxProTagTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    // 测试同id的节点在when场景中tag是否正常
    @Test
    public void testTag1() throws Exception {
        for (int i = 0; i < 1; i++) {
            LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
            DefaultContext context = response.getFirstContextBean();
            Assertions.assertTrue(response.isSuccess());
            ConcurrentHashSet<String> testSet = context.getData("test");
            Assertions.assertEquals(5, testSet.size());
        }
    }
}