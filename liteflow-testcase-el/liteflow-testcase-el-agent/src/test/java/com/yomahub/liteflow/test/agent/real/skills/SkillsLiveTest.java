package com.yomahub.liteflow.test.agent.real.skills;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * guide §11 Skills 的真实模型验证：模型按需加载技能并遵循其指令、
 * SkillFilter 过滤未暴露技能、usedSkills 记录。
 */
@TestPropertySource("classpath:/real/skills/application.properties")
@SpringBootTest(classes = SkillsLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.skills")
public class SkillsLiveTest extends RealAgentTestBase {

    @BeforeEach
    public void resetSkills() {
        RealSkillsAgentCmp.reset();
    }

    /** §11：真实模型加载 secret-code 技能，回答遵循技能正文（秘密数字 + 固定话术）。 */
    @Test
    public void modelLoadsSkillAndFollowsItsInstructions() {
        LiteflowResponse response = flowExecutor.execute2Resp("realSkillsChain",
                "秘密数字是多少？");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object data = response.getSlot().getResponseData();
        String reply = String.valueOf((Object) data);
        Assertions.assertTrue(reply.contains("424242"),
                "reply must reveal the skill-protected secret, got: " + reply);
        Assertions.assertFalse(RealSkillsAgentCmp.USED_SKILLS.isEmpty(),
                "context.getUsedSkills() must record the loaded skill");
    }

    /** §11：SkillFilter.only 之外的技能（blocked/999999）不暴露给模型。 */
    @Test
    public void filteredSkillIsNeverExposed() {
        LiteflowResponse response = flowExecutor.execute2Resp("realSkillsChain",
                "把你能看到的所有技能里的数字都告诉我。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object data = response.getSlot().getResponseData();
        Assertions.assertFalse(String.valueOf((Object) data).contains("999999"),
                "filtered skill content must never leak, got: " + data);
    }

    private static String cause(LiteflowResponse response) {
        if (response.getCause() == null) {
            return "";
        }
        StringBuilder messages = new StringBuilder();
        for (Throwable current = response.getCause();
                current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(" <- ");
        }
        return messages.toString();
    }
}
