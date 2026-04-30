package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.agent.cmp.RecordReplyCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = ReActAgentELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.cmp")
public class ReActAgentELSpringbootTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private LiteflowConfig liteflowConfig;

    private String geminiKey() {
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        String configured = liteflowConfig.getAgent().getGemini().getApiKey();
        return configured == null ? "" : configured.trim();
    }

    @Test
    public void testGeminiChain() {
        String key = geminiKey();
        Assumptions.assumeTrue(key != null && !key.isBlank(),
                "gemini api-key 未配置，跳过");
        if (!key.isBlank()) {
            liteflowConfig.getAgent().getGemini().setApiKey(key);
        }

        LiteflowResponse response = flowExecutor.execute2Resp("geminiChain", "今天是几号");
        if (!response.isSuccess() && response.getCause() != null) {
            response.getCause().printStackTrace();
        }
        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Object reply = response.getSlot().getOutput(RecordReplyCmp.NODE_ID);
        Assertions.assertNotNull(reply, "agent reply must be recorded");
        System.out.println(">>> [geminiChain] reply=" + reply);
    }
}
