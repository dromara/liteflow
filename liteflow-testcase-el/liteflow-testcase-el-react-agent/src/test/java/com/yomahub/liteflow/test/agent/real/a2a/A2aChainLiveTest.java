package com.yomahub.liteflow.test.agent.real.a2a;

import com.yomahub.liteflow.agent.a2a.A2aAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.agentscope.core.a2a.agent.card.AgentCardResolver;
import io.agentscope.core.a2a.agent.card.FixedAgentCardResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

/**
 * guide §13 A2A 客户端 的真实协议验证：内嵌最小 A2A 服务端，
 * A2aAgentComponent 经标准 JSON-RPC 调用远程 Agent 并取回文本回复。
 */
@TestPropertySource("classpath:/real/a2a/application.properties")
@SpringBootTest(classes = A2aChainLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.a2a")
public class A2aChainLiveTest extends RealAgentTestBase {

    private static MinimalA2aServer server;

    @BeforeAll
    static void startRemoteAgent() throws Exception {
        server = new MinimalA2aServer();
    }

    @AfterAll
    static void stopRemoteAgent() {
        if (server != null) {
            server.close();
        }
    }

    /** §13：调用远程 Agent，回复写入 responseData，请求携带 LiteFlow 元数据。 */
    @Test
    public void remoteAgentCallSucceedsOverRealA2aProtocol() {
        LiteflowResponse response = flowExecutor.execute2Resp("realA2aChain",
                "你好远程代理");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object data = response.getSlot().getResponseData();
        Assertions.assertTrue(String.valueOf((Object) data).contains("A2A-REPLY-OK"),
                "reply must come from the remote agent, got: " + data);
        Assertions.assertEquals(1, server.receivedTexts.size());
        Assertions.assertEquals("你好远程代理", server.receivedTexts.get(0));

        Assertions.assertFalse(server.receivedMetadata.isEmpty(), "metadata must be sent");
        // A2A SDK 将 metadata 包一层 message id：{<messageId>: {liteflow.*: ...}}
        Map<String, Object> wrapped = server.receivedMetadata.get(0);
        Map<String, Object> metadata = nestedMetadata(wrapped);
        Assertions.assertTrue(metadata.containsKey("liteflow.userId"), "meta: " + wrapped);
        Assertions.assertTrue(metadata.containsKey("liteflow.conversationId"), "meta: " + wrapped);
        Assertions.assertTrue(metadata.containsKey("liteflow.agentKey"), "meta: " + wrapped);
        Assertions.assertTrue(metadata.containsKey("liteflow.traceId"), "meta: " + wrapped);
    }

    /** §13 / §18：远端返回 JSON-RPC error → 链失败并携带原因。 */
    @Test
    public void remoteErrorBecomesLiteFlowFailure() {
        server.failNext = true;

        LiteflowResponse response = flowExecutor.execute2Resp("realA2aChain",
                "触发远端错误");

        Assertions.assertFalse(response.isSuccess(), "remote error must fail the chain");
        Assertions.assertTrue(cause(response).contains("a2a upstream failure"),
                "unexpected cause: " + cause(response));
    }

    @org.springframework.stereotype.Component("realA2aAgent")
    static class RealA2aAgentCmp extends A2aAgentComponent {

        @Override
        protected String remoteAgentName() {
            return "remote-assistant";
        }

        @Override
        protected AgentCardResolver agentCardResolver() {
            AgentCard card = new AgentCard.Builder()
                    .name("remote-assistant")
                    .description("minimal test agent")
                    .url(server.baseUrl())
                    .version("1.0.0")
                    .capabilities(new AgentCapabilities(false, false, false, List.of()))
                    .defaultInputModes(List.of("text"))
                    .defaultOutputModes(List.of("text"))
                    .skills(List.of())
                    .preferredTransport("JSONRPC")
                    .protocolVersion("0.3.0")
                    .build();
            return FixedAgentCardResolver.builder().agentCard(card).build();
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            return reqData == null ? "" : reqData.toString();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nestedMetadata(Map<String, Object> wrapped) {
        if (wrapped.keySet().stream().anyMatch(k -> k.startsWith("liteflow."))) {
            return wrapped;
        }
        return wrapped.values().stream()
                .filter(Map.class::isInstance)
                .map(value -> (Map<String, Object>) value)
                .findFirst()
                .orElse(wrapped);
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
