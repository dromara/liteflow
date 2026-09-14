package com.yomahub.liteflow.test.agent.real.harness;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.TextBlock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * guide §12 Harness 模块 的真实模型验证（GUARDED_LOCAL 后端）：
 * 文件系统工具、上下文压缩、长期记忆、工具结果淘汰、子代理与计划模式。
 */
@TestPropertySource("classpath:/real/harness/application.properties")
@SpringBootTest(classes = HarnessLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.harness")
public class HarnessLiveTest extends RealAgentTestBase {

    private static final String WORKSPACE_ROOT = "target/wk/real_harness";

    /** §12：模型经 harness 文件系统在 workspace 写入真实文件。 */
    @Test
    public void harnessFilesystemWritesRealFile() throws Exception {
        String marker = "HARNESS-FILE-" + UUID.randomUUID();
        String filename = "harness-" + UUID.randomUUID() + ".txt";
        LiteflowResponse response = flowExecutor.execute2Resp("realHarnessFileChain",
                "请把文本 " + marker + " 写入 " + filename + " 文件（使用你的文件工具），"
                        + "然后告诉我完成情况。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        try (Stream<Path> paths = Files.walk(Path.of(WORKSPACE_ROOT))) {
            boolean found = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(filename))
                    .anyMatch(path -> containsMarker(path, marker));
            Assertions.assertTrue(found,
                    "harness workspace must contain the requested new file, not just a session log");
        }
    }

    /** §12 对照：关闭全部可选能力的 harness Agent 也能正常工作。 */
    @Test
    public void minimalHarnessAgentStillAnswers() {
        LiteflowResponse response = flowExecutor.execute2Resp("realHarnessMinimalChain",
                "用一句话说明什么是 Docker。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Assertions.assertFalse(
                String.valueOf((Object) response.getSlot().getResponseData()).isBlank());
    }

    /** §12.1：多轮对话跨过 triggerMessages 阈值后触发压缩，最近记忆仍可用。 */
    @Test
    public void compactionKeepsRecentMemoryAcrossManyTurns() {
        String cid = "real-compaction-" + UUID.randomUUID();
        int turns = 4; // Seven messages before turn four cross triggerMessages=6.
        for (int turn = 1; turn <= turns; turn++) {
            LiteflowResponse response = flowExecutor.execute2Resp("realCompactionChain",
                    "这是第 " + turn + " 轮。请只回复数字：" + turn,
                    ExecuteOption.of().conversationId(cid));
            Assertions.assertTrue(response.isSuccess(),
                    "turn " + turn + " failed: " + cause(response));
        }
        LiteflowResponse recall = flowExecutor.execute2Resp("realCompactionChain",
                "我们刚进行到第几轮？只回复数字。",
                ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(recall.isSuccess(), cause(recall));
        Assertions.assertTrue(
                String.valueOf((Object) recall.getSlot().getResponseData()).contains(String.valueOf(turns)),
                "recent memory must survive compaction, got: "
                        + recall.getSlot().getResponseData());
        try (var history = AgentConversationService.open(liteflowConfig.getAgent())) {
            var state = history.agentState(liteflowConfig.getAgent().getRuntime().getDefaultUserId(),
                    cid, "realCompactionAgent").orElseThrow();
            Assertions.assertTrue(state.getContext().size() < 2 * (turns + 1),
                    "The user/assistant turns must actually be compacted, not merely recalled from full history");
        }
    }

    /** §12.2：长期记忆抽取（真实模型）执行成功且最终回复可用。 */
    @Test
    public void longTermMemoryExtractionSucceeds() throws Exception {
        String cid = "real-memory-" + UUID.randomUUID();
        String marker = "NEBULA-" + UUID.randomUUID();
        LiteflowResponse response = flowExecutor.execute2Resp("realMemoryChain",
                "请记住：我的项目代号是 " + marker + "。请简短确认。",
                ExecuteOption.of().conversationId(cid));

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Assertions.assertFalse(
                String.valueOf((Object) response.getSlot().getResponseData()).isBlank());
        try (Stream<Path> paths = Files.walk(Path.of(WORKSPACE_ROOT))) {
            Assertions.assertTrue(paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("MEMORY.md")
                            || path.toString().contains("/memory/"))
                    .anyMatch(path -> containsMarker(path, marker)),
                    "The unique fact must be persisted in long-term memory, not just repeated in the answer");
        }
    }

    /** §12.3：超大工具结果被淘汰（只保留预览），模型仍能完成回答。 */
    @Test
    public void oversizedToolResultIsEvictedButAgentCompletes() {
        LiteflowResponse response = flowExecutor.execute2Resp("realEvictionChain",
                "请调用工具获取报告并告诉我报告的开头关键词。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object data = response.getSlot().getResponseData();
        Assertions.assertTrue(String.valueOf((Object) data).contains("REPORT-HEADER"),
                "reply must reference the preview header, got: " + data);
        try (var history = AgentConversationService.open(liteflowConfig.getAgent())) {
            var state = history.agentState(liteflowConfig.getAgent().getRuntime().getDefaultUserId(),
                    response.getConversationId(), "realEvictionAgent").orElseThrow();
            var results = state.getContext().stream().flatMap(message -> message.getContent().stream())
                    .filter(ToolResultBlock.class::isInstance).map(ToolResultBlock.class::cast)
                    .flatMap(block -> block.getOutput().stream()).filter(TextBlock.class::isInstance)
                    .map(TextBlock.class::cast).map(TextBlock::getText).toList();
            Assertions.assertFalse(results.isEmpty(), "The model must actually receive a tool result");
            Assertions.assertTrue(results.stream().allMatch(text -> text.length() < 40_000),
                    "The full 40K report must be evicted from working context");
        }
    }

    /** GUARDED_LOCAL 禁止实际子代理执行；这里只验证声明和计划配置，不据此宣称已验证委派。 */
    @Test
    public void subagentAndPlanModeChainCompletes() {
        LiteflowResponse response = flowExecutor.execute2Resp("realSubagentPlanChain",
                "请完成一个小调研：总结使用规则引擎的三个好处。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object data = response.getSlot().getResponseData();
        Assertions.assertFalse(String.valueOf((Object) data).isBlank());
    }

    private static boolean containsMarker(Path file, String marker) {
        try {
            return Files.size(file) < 1024 * 1024
                    && Files.readString(file).contains(marker);
        } catch (Exception e) {
            return false;
        }
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
