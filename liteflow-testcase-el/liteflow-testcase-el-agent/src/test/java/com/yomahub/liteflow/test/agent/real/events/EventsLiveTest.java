package com.yomahub.liteflow.test.agent.real.events;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.FlowEvent;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.agent.AgentListenerFailureMode;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * guide §6 流式输出与事件监听 的真实模型验证：
 * 文本增量事件、工具事件、agent.result 终止事件、ChatUsage 统计、
 * 监听器异常策略（FAIL_FAST / LOG_AND_CONTINUE）。
 */
@TestPropertySource("classpath:/real/events/application.properties")
@SpringBootTest(classes = EventsLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.events")
public class EventsLiveTest extends RealAgentTestBase {

    private final List<FlowEvent> events = new CopyOnWriteArrayList<>();

    @BeforeEach
    public void resetCaptures() {
        events.clear();
        RealEventsAgentsCmp.reset();
    }

    /** §6.1：agent.start / 多段 agent.text.delta / 单个 last=true 的 agent.result。 */
    @Test
    public void textDeltaEventsStreamDuringExecution() {
        LiteflowResponse response = flowExecutor.execute2Resp("realEventsChain",
                "请用两三句话介绍一下杭州。", ExecuteOption.of().eventListener(events::add));

        Assertions.assertTrue(response.isSuccess(), cause(response));
        List<String> types = events.stream().map(FlowEvent::getType).toList();
        Assertions.assertTrue(types.contains("agent.start"), "agent.start missing: " + types);
        Assertions.assertTrue(types.contains("agent.end"), "agent.end missing: " + types);
        Assertions.assertTrue(types.contains("agent.text.delta"),
                "agent.text.delta missing: " + types);
        Assertions.assertEquals(1, types.stream().filter("agent.result"::equals).count(),
                "exactly one agent.result expected: " + types);
        Assertions.assertTrue(events.stream().anyMatch(FlowEvent::isLast),
                "agent.result must carry last=true");

        String streamed = events.stream()
                .filter(e -> "agent.text.delta".equals(e.getType()))
                .map(FlowEvent::getText)
                .reduce("", String::concat);
        Object reply = response.getSlot().getResponseData();
        Assertions.assertNotNull(reply);
        Assertions.assertFalse(streamed.isBlank(), "deltas must not be blank");
        Assertions.assertEquals(String.valueOf(reply).strip(), streamed.strip(),
                "concatenated deltas must equal the final reply");
    }

    /** §6.1：工具调用触发 agent.tool.call.start / end 与 agent.tool.result.*。 */
    @Test
    public void toolCallEventsAreEmitted() {
        LiteflowResponse response = flowExecutor.execute2Resp("realEventsToolChain",
                "北京今天天气怎么样？", ExecuteOption.of().eventListener(events::add));

        Assertions.assertTrue(response.isSuccess(), cause(response));
        List<String> types = events.stream().map(FlowEvent::getType).toList();
        Assertions.assertTrue(types.contains("agent.tool.call.start"),
                "tool.call.start missing: " + types);
        Assertions.assertTrue(types.contains("agent.tool.call.end"),
                "tool.call.end missing: " + types);
        Assertions.assertTrue(types.contains("agent.tool.result.end"),
                "tool.result.end missing: " + types);
        Object replyData = response.getSlot().getResponseData();
        String reply = String.valueOf(replyData);
        Assertions.assertTrue(reply.contains("26") || reply.contains("晴"),
                "reply must be grounded in tool output, got: " + reply);
    }

    /** §6.2：ChatUsage 的 input/output token 均被累计且大于 0。 */
    @Test
    public void chatUsageIsAccumulatedAndExposed() {
        LiteflowResponse response = flowExecutor.execute2Resp("realEventsToolChain",
                "上海今天天气如何？一句话。", ExecuteOption.of().eventListener(events::add));

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Assertions.assertEquals(1, RealEventsAgentsCmp.CAPTURED_USAGES.size(),
                "handleReply must observe exactly one accumulated usage");
        var usage = RealEventsAgentsCmp.CAPTURED_USAGES.get(0);
        Assertions.assertNotNull(usage, "ChatUsage must be available in handleReply");
        Assertions.assertTrue(usage.getInputTokens() > 0, "input tokens must be positive");
        Assertions.assertTrue(usage.getOutputTokens() > 0, "output tokens must be positive");
        Assertions.assertEquals(usage.getInputTokens() + usage.getOutputTokens(),
                usage.getTotalTokens(), "total tokens must equal input + output");
    }

    /** §6.1 FAIL_FAST（默认）：监听器抛异常中断执行，链失败。 */
    @Test
    public void listenerFailureFailFastBreaksTheChain() {
        liteflowConfig.getAgent().getEvent()
                .setListenerFailureMode(AgentListenerFailureMode.FAIL_FAST);
        LiteflowResponse response = flowExecutor.execute2Resp("realEventsStrictChain",
                "你好", ExecuteOption.of().eventListener(event -> {
                    if ("agent.text.delta".equals(event.getType())) {
                        throw new IllegalStateException("listener blew up");
                    }
                }));

        Assertions.assertFalse(response.isSuccess(), "FAIL_FAST must fail the chain");
        Assertions.assertTrue(cause(response).contains("listener blew up"),
                "unexpected cause: " + cause(response));
    }

    /**
     * §6.1 LOG_AND_CONTINUE：监听器异常被吞掉，链仍成功。
     * 事件桥在 runtime 构建期固化失败模式，故使用专用组件并先设配置。
     */
    @Test
    public void listenerFailureLogAndContinueKeepsTheChain() {
        liteflowConfig.getAgent().getEvent()
                .setListenerFailureMode(AgentListenerFailureMode.LOG_AND_CONTINUE);
        LiteflowResponse response = flowExecutor.execute2Resp("realEventsTolerantChain",
                "你好", ExecuteOption.of().eventListener(event -> {
                    if ("agent.text.delta".equals(event.getType())) {
                        throw new IllegalStateException("listener blew up");
                    }
                }));

        Assertions.assertTrue(response.isSuccess(),
                "LOG_AND_CONTINUE must keep the chain alive: " + cause(response));
        Object data = response.getSlot().getResponseData();
        Assertions.assertFalse(String.valueOf(data).isBlank());
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
