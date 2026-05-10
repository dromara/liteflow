package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.StubReActAgentCmp;
import com.yomahub.liteflow.test.agent.tool.SkillEchoTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ReActAgentSkillIntegrationTest extends AbstractReActAgentSpringbootTest {

    @Test
    public void testSkillsDisabledKeepsExistingToolSet() {
        liteflowConfig.getAgent().getSkills().setEnabled(false);

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "tools");

        Assertions.assertTrue(response.isSuccess());
        List<String> toolNames = StubReActAgentCmp.MODEL_PROBES.get(0).toolNames();
        Assertions.assertFalse(toolNames.contains("load_skill_through_path"));
    }

    @Test
    public void testSkillsEnabledAddsSkillLoadTool() {
        enableTestSkills();

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "skills");

        Assertions.assertTrue(response.isSuccess());
        List<String> toolNames = StubReActAgentCmp.MODEL_PROBES.get(0).toolNames();
        Assertions.assertTrue(toolNames.contains("load_skill_through_path"));
    }

    @Test
    public void testComponentSkillAllowListStillBuildsAgent() {
        enableTestSkills();
        StubReActAgentCmp.allowedSkills = List.of("demo");

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "filtered-skills");

        Assertions.assertTrue(response.isSuccess());
        List<String> toolNames = StubReActAgentCmp.MODEL_PROBES.get(0).toolNames();
        Assertions.assertTrue(toolNames.contains("load_skill_through_path"));
    }

    @Test
    public void testMissingComponentSkillFailsInStrictMode() {
        enableTestSkills();
        StubReActAgentCmp.allowedSkills = List.of("missing-skill");

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "missing-skill");

        Assertions.assertFalse(response.isSuccess());
        Assertions.assertTrue(response.getMessage().contains("missing-skill"));
    }

    @Test
    public void testSkillFrontmatterToolIsInstantiatedDuringAgentBuild() {
        enableTestSkills();
        StubReActAgentCmp.allowedSkills = List.of("tool-skill");
        SkillEchoTool.reset();

        LiteflowResponse response = flowExecutor.execute2Resp("stubAgentChain", "tool-skill");

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(1, SkillEchoTool.CONSTRUCT_COUNT.get());
    }

    @Test
    public void testCachedAgentKeepsInitialSkillAllowList() {
        enableTestSkills();
        StubReActAgentCmp.allowedSkills = List.of("demo");

        LiteflowResponse first = flowExecutor.execute2Resp("stubAgentChain", "cache-skills-first");

        Assertions.assertTrue(first.isSuccess());
        Assertions.assertEquals(1, StubReActAgentCmp.SPEC_RESOLVE_COUNT.get());

        StubReActAgentCmp.allowedSkills = List.of("missing-skill");
        LiteflowResponse second = flowExecutor.execute2Resp("stubAgentChain", "cache-skills-second");

        Assertions.assertTrue(second.isSuccess());
        Assertions.assertEquals(1, StubReActAgentCmp.SPEC_RESOLVE_COUNT.get());
    }

    @Test
    public void testUsedSkillsTracksInvocationAndClearsForCachedAgent() {
        enableTestSkills();
        StubReActAgentCmp.allowedSkills = List.of("demo");

        LiteflowResponse first = flowExecutor.execute2Resp("stubAgentChain", "load-demo-skill");

        Assertions.assertTrue(first.isSuccess());
        Assertions.assertEquals(List.of("demo"), StubReActAgentCmp.USED_SKILL_PROBES.get(0));

        LiteflowResponse second = flowExecutor.execute2Resp("stubAgentChain", "no-skill-load");

        Assertions.assertTrue(second.isSuccess());
        Assertions.assertEquals(List.of(), StubReActAgentCmp.USED_SKILL_PROBES.get(1));
    }

    private void enableTestSkills() {
        liteflowConfig.getAgent().getSkills().setEnabled(true);
        liteflowConfig.getAgent().getSkills().setPath("src/test/resources/agent/skills");
        liteflowConfig.getAgent().getSkills().setStrict(true);
    }
}
