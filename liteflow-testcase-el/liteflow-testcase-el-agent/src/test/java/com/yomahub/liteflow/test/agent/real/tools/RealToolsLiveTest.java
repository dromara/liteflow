package com.yomahub.liteflow.test.agent.real.tools;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * guide §4 工具 的真实模型验证：自定义 @Tool、Spring bean 工具、
 * 内置文件工具与 shell 工具（白名单模式）。
 */
@TestPropertySource("classpath:/real/tools/application.properties")
@SpringBootTest(classes = RealToolsLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.tools")
public class RealToolsLiveTest extends RealAgentTestBase {

    private static final String WORKSPACE = "target/wk/real_tools";

    @BeforeEach
    public void resetLedger() {
        RealToolboxCmp.ToolLedger.reset();
    }

    /** §4.1：真实模型按需调用自定义 @Tool 并基于结果作答。 */
    @Test
    public void modelCallsCustomToolAndAnswersFromResult() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "realOrderChain", "帮我查一下订单 10001 现在的状态，一句话告诉我。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Assertions.assertEquals(1, RealToolboxCmp.ToolLedger.ORDER_HITS.get(),
                "model must invoke query_order exactly once, calls="
                        + RealToolboxCmp.ToolLedger.calls());
        Object replyData = response.getSlot().getResponseData();
        String reply = String.valueOf(replyData);
        Assertions.assertTrue(reply.contains("已发货"),
                "reply must be grounded in tool result, got: " + reply);
    }

    /** §4.2：Spring bean 工具（构造注入 service）被模型调用。 */
    @Test
    public void springBeanToolIsInvokedByModel() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "realBeanToolChain", "请用工具向小明生成一句欢迎语并转告我。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Assertions.assertTrue(RealToolboxCmp.ToolLedger.calls().stream()
                        .anyMatch(call -> call.startsWith("greet_user:小明")),
                "greet_user tool must be called with 小明, calls="
                        + RealToolboxCmp.ToolLedger.calls());
        Object replyData = response.getSlot().getResponseData();
        String reply = String.valueOf(replyData);
        Assertions.assertTrue(reply.contains("小明") && reply.contains("欢迎"),
                "reply must carry the greeting, got: " + reply);
    }

    /** §4.3：内置文件工具 write/view 真实落盘且内容可读回。 */
    @Test
    public void workspaceFileToolsWriteAndReadRealFile() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "realFileToolsChain",
                "请完成两步：1) 用文件工具把文本 LITEFLOW-FILE-OK 写入 notes/real.txt；"
                        + "2) 再读取该文件并告诉我其中的原文。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Path written = Path.of(WORKSPACE, "notes", "real.txt");
        Assertions.assertTrue(Files.exists(written), "file must be written into workspace");
        String content;
        try {
            content = Files.readString(written);
        } catch (Exception e) {
            throw new IllegalStateException("failed reading workspace file", e);
        }
        Assertions.assertTrue(content.contains("LITEFLOW-FILE-OK"),
                "file content mismatch: " + content);
        Object replyData = response.getSlot().getResponseData();
        String reply = String.valueOf(replyData);
        Assertions.assertTrue(reply.contains("LITEFLOW-FILE-OK"),
                "reply must quote file content, got: " + reply);
    }

    /** §4.3：内置 shell 工具在白名单模式下执行命令并回传输出。 */
    @Test
    public void shellToolRunsWhitelistedCommand() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "realShellToolChain",
                "请用 shell 工具执行命令 echo LITEFLOW-SHELL-OK 并把输出原样告诉我。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object replyData = response.getSlot().getResponseData();
        String reply = String.valueOf(replyData);
        Assertions.assertTrue(reply.contains("LITEFLOW-SHELL-OK"),
                "reply must contain echo output, got: " + reply);
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

    @SuppressWarnings("unused")
    private static long countWorkspaceFiles() throws Exception {
        try (Stream<Path> paths = Files.walk(Path.of(WORKSPACE))) {
            return paths.filter(Files::isRegularFile).count();
        }
    }
}
