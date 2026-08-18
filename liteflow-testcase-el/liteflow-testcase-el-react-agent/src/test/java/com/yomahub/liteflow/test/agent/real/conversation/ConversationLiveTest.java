package com.yomahub.liteflow.test.agent.real.conversation;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * guide §5 多轮对话与状态持久化（JSON 后端）的真实模型验证：
 * conversationId 四种指定方式、多轮记忆、落盘结构、agentKey 记忆隔离。
 */
@TestPropertySource("classpath:/real/conversation/application.properties")
@SpringBootTest(classes = ConversationLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.conversation")
public class ConversationLiveTest extends RealAgentTestBase {

    private static final String JSON_ROOT = "target/agent-state-real/conversation";

    private static final String REMEMBER_PROMPT =
            "请记住暗号 %s，回复“好的”即可。";
    private static final String RECALL_PROMPT =
            "我们这个会话的暗号是什么？只回复暗号本身。";

    /** §5.2 方式一：ExecuteOption 显式指定 conversationId，两轮共享记忆。 */
    @Test
    public void explicitConversationIdCarriesMemoryAcrossTurns() {
        String cid = "real-cid-" + UUID.randomUUID();
        LiteflowResponse first = flowExecutor.execute2Resp("realSecretChain",
                REMEMBER_PROMPT.formatted("PINEAPPLE-77"),
                ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(first.isSuccess(), cause(first));
        Assertions.assertEquals(cid, first.getConversationId());

        LiteflowResponse second = flowExecutor.execute2Resp("realSecretChain",
                RECALL_PROMPT, ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(second.isSuccess(), cause(second));
        Assertions.assertTrue(reply(second).contains("PINEAPPLE-77"),
                "second turn must recall the secret, got: " + reply(second));
    }

    /** §5.2 方式二：autoConversationId 生成后取回复用。 */
    @Test
    public void autoConversationIdCanBeReused() {
        LiteflowResponse first = flowExecutor.execute2Resp("realSecretChain",
                REMEMBER_PROMPT.formatted("MANGO-2026"),
                ExecuteOption.of().autoConversationId());
        Assertions.assertTrue(first.isSuccess(), cause(first));
        String cid = first.getConversationId();
        Assertions.assertFalse(cid == null || cid.isBlank(),
                "autoConversationId must be returned on the response");

        LiteflowResponse second = flowExecutor.execute2Resp("realSecretChain",
                RECALL_PROMPT, ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(second.isSuccess(), cause(second));
        Assertions.assertTrue(reply(second).contains("MANGO-2026"),
                "reused auto cid must recall the secret, got: " + reply(second));
    }

    /** §5.2 方式三：请求参数 Map 携带 conversationId。 */
    @Test
    public void conversationIdFromRequestMap() {
        String cid = "real-map-cid-" + UUID.randomUUID();
        LiteflowResponse first = flowExecutor.execute2Resp("realSecretChain",
                Map.of("conversationId", cid, "text", REMEMBER_PROMPT.formatted("GRAPE-409")));
        Assertions.assertTrue(first.isSuccess(), cause(first));
        Assertions.assertEquals(cid, first.getConversationId());

        LiteflowResponse second = flowExecutor.execute2Resp("realSecretChain",
                Map.of("conversationId", cid, "text", RECALL_PROMPT));
        Assertions.assertTrue(second.isSuccess(), cause(second));
        Assertions.assertTrue(reply(second).contains("GRAPE-409"),
                "map-provided cid must recall the secret, got: " + reply(second));
    }

    /** §5.2 方式四：组件覆写 resolveConversationId。 */
    @Test
    public void resolveConversationIdOverrideWorks() {
        String cid = "real-resolved-cid-" + UUID.randomUUID();
        RealConversationCmp.FixedCidAgentCmp.FIXED_CID = cid;
        try {
            LiteflowResponse first = flowExecutor.execute2Resp("realFixedCidChain",
                    REMEMBER_PROMPT.formatted("CHERRY-888"));
            Assertions.assertTrue(first.isSuccess(), cause(first));
            Assertions.assertEquals(cid, first.getConversationId(),
                    "resolved conversationId must be reported");

            LiteflowResponse second = flowExecutor.execute2Resp("realFixedCidChain",
                    RECALL_PROMPT);
            Assertions.assertTrue(second.isSuccess(), cause(second));
            Assertions.assertTrue(reply(second).contains("CHERRY-888"),
                    "resolved cid must recall the secret, got: " + reply(second));
        } finally {
            RealConversationCmp.FixedCidAgentCmp.FIXED_CID = "";
        }
    }

    /**
     * §5.3：JSON 后端真实落盘。
     *
     * <p>注意：当前实现（AgentScope 2.0.2 JsonFileAgentStateStore）把对话历史
     * 持久化在 agent_state.json 的 context 字段内；指南 §5.3 描述的独立
     * memory_messages.jsonl 文件实际不会生成——本用例按真实行为断言，
     * 差异已记入测试报告。
     */
    @Test
    public void jsonStateStorePersistsConversationOnDisk() throws Exception {
        String cid = "real-disk-cid-" + UUID.randomUUID();
        LiteflowResponse first = flowExecutor.execute2Resp("realSecretChain",
                REMEMBER_PROMPT.formatted("DURIAN-321"), ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(first.isSuccess(), cause(first));

        try (Stream<Path> paths = Files.walk(Path.of(JSON_ROOT))) {
            Path stateFile = paths
                    .filter(p -> p.getFileName().toString().equals("agent_state.json"))
                    .filter(ConversationLiveTest::containsSecret)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "no agent_state.json containing the secret under " + JSON_ROOT));
            Path sessionDir = stateFile.getParent();
            String dirName = sessionDir.getFileName().toString();
            Assertions.assertTrue(dirName.startsWith("lf-") && dirName.contains(".lf-"),
                    "session dir must follow lf-<agentHash>.lf-<conversationHash>, got: "
                            + dirName);
            Assertions.assertTrue(sessionDir.getParent().getFileName().toString()
                            .equals("anonymous"),
                    "user dir must default to anonymous");
        }
    }

    /**
     * §5.1 / §9.5：同一 conversationId 下不同 Agent（不同 agentKey）记忆互不干扰。
     */
    @Test
    public void agentKeyIsolatesMemoryBetweenAgents() {
        String cid = "real-iso-cid-" + UUID.randomUUID();
        LiteflowResponse teach = flowExecutor.execute2Resp("realIsoAChain",
                REMEMBER_PROMPT.formatted("KIWI-SECRET"), ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(teach.isSuccess(), cause(teach));

        LiteflowResponse probe = flowExecutor.execute2Resp("realIsoBChain",
                "我们这个会话的暗号是什么？如果你在对话历史中找不到，只回复 NO_MEMORY。",
                ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(probe.isSuccess(), cause(probe));
        String answer = reply(probe);
        Assertions.assertFalse(answer.contains("KIWI-SECRET"),
                "agent B must NOT see agent A's memory, got: " + answer);
    }

    private static boolean containsSecret(Path jsonl) {
        try {
            return Files.readString(jsonl).contains("DURIAN-321");
        } catch (Exception e) {
            return false;
        }
    }

    private static String reply(LiteflowResponse response) {
        Object data = response.getSlot().getResponseData();
        return data == null ? "" : String.valueOf(data);
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
