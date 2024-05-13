package com.yomahub.liteflow.test.script.kotlin.meta;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.script.BaseTest;
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
@TestPropertySource(value = "classpath:/meta/application.properties")
@SpringBootTest(classes = LiteflowKotlinScriptMetaELTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.script.kotlin.meta.cmp"})
public class LiteflowKotlinScriptMetaELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testMeta() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("chain1", context.getData("currChainId"));
        Assertions.assertEquals("arg", context.getData("requestData"));
        Assertions.assertEquals("s1", context.getData("nodeId"));
    }
}
