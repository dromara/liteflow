package com.yomahub.liteflow.test.agent.feature.harnessskills;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource("classpath:/feature/harnessskills/application.properties")
@SpringBootTest(classes = HarnessSkillsIntegrationTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.harnessskills")
class HarnessSkillsIntegrationTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private LiteflowConfig liteflowConfig;

    @BeforeEach
    void configureRepository() {
        HarnessSkillsAgentCmp.reset();
        liteflowConfig.getAgent().getSkills().setEnabled(true);
        liteflowConfig.getAgent().getSkills().setStrict(true);
        liteflowConfig.getAgent().getSkills().setPath(resolveSkillsPath());
    }

    @Test
    void repositoryFilterReachesHarnessAndSuccessfulLoadIsTracked() {
        LiteflowResponse response = flowExecutor.execute2Resp("harnessSkillsChain", "load demo");

        assertTrue(response.isSuccess());
        assertTrue(HarnessSkillsAgentCmp.renderedPrompt().contains("DEMO-HARNESS-SKILL"));
        assertFalse(HarnessSkillsAgentCmp.renderedPrompt().contains("BLOCKED-HARNESS-SKILL"));
        assertEquals(List.of(HarnessSkillsAgentCmp.allowedSkillId()),
                HarnessSkillsAgentCmp.usedSkills());
        assertEquals(1, HarnessSkillsAgentCmp.successfulLoadResults());
    }

    private static String resolveSkillsPath() {
        Path local = Path.of("src/test/resources/feature/harnessskills/skills");
        if (Files.isDirectory(local)) {
            return local.toAbsolutePath().normalize().toString();
        }
        return Path.of("liteflow-testcase-el/liteflow-testcase-el-agent",
                        "src/test/resources/feature/harnessskills/skills")
                .toAbsolutePath()
                .normalize()
                .toString();
    }
}
