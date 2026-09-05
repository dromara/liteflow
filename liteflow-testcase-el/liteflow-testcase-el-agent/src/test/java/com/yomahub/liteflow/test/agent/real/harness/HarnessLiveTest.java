package com.yomahub.liteflow.test.agent.real.harness;

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
        LiteflowResponse response = flowExecutor.execute2Resp("realHarnessFileChain",
                "请把文本 HARNESS-FILE-OK 写入 harness-note.txt 文件（使用你的文件工具），"
                        + "然后告诉我完成情况。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        try (Stream<Path> paths = Files.walk(Path.of(WORKSPACE_ROOT))) {
            boolean found = paths
                    .filter(Files::isRegularFile)
                    .anyMatch(HarnessLiveTest::containsHarnessMarker);
            Assertions.assertTrue(found,
                    "harness workspace must contain a file with HARNESS-FILE-OK");
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
        for (int turn = 1; turn <= 8; turn++) {
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
                String.valueOf((Object) recall.getSlot().getResponseData()).contains("8"),
                "recent memory must survive compaction, got: "
                        + recall.getSlot().getResponseData());
    }

    /** §12.2：长期记忆抽取（真实模型）执行成功且最终回复可用。 */
    @Test
    public void longTermMemoryExtractionSucceeds() {
        String cid = "real-memory-" + UUID.randomUUID();
        LiteflowResponse response = flowExecutor.execute2Resp("realMemoryChain",
                "请记住：我的项目代号是 NEBULA-7。请简短确认。",
                ExecuteOption.of().conversationId(cid));

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Assertions.assertFalse(
                String.valueOf((Object) response.getSlot().getResponseData()).isBlank());
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
    }

    /** §12.4：子代理声明 + 计划模式开启的复杂任务链路。 */
    @Test
    public void subagentAndPlanModeChainCompletes() {
        LiteflowResponse response = flowExecutor.execute2Resp("realSubagentPlanChain",
                "请完成一个小调研：总结使用规则引擎的三个好处。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object data = response.getSlot().getResponseData();
        Assertions.assertFalse(String.valueOf((Object) data).isBlank());
    }

    private static boolean containsHarnessMarker(Path file) {
        try {
            return Files.size(file) < 1024 * 1024
                    && Files.readString(file).contains("HARNESS-FILE-OK");
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
