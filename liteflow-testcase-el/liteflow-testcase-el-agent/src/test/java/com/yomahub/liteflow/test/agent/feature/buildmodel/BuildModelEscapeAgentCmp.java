package com.yomahub.liteflow.test.agent.feature.buildmodel;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 演示 guide §4.6 buildModel() 逃生舱：完全自行构造确定性的 AgentScope Model。
 * 验证覆写 buildModel() 后 {@code model().resolve(...)} 不再被调用。
 */
@Component("buildModelEscapeAgent")
public class BuildModelEscapeAgentCmp extends HarnessAgentComponent {

    public static final AtomicInteger BUILD_MODEL_COUNT = new AtomicInteger();

    public static void reset() {
        BUILD_MODEL_COUNT.set(0);
    }

    @Override
    protected ModelSpec<?> model() {
        throw new AssertionError("buildModel override must bypass model().resolve(...)");
    }

    @Override
    protected Model buildModel() {
        BUILD_MODEL_COUNT.incrementAndGet();
        return new DeterministicModel();
    }

    @Override
    protected String systemPrompt() {
        return "你是 LiteFlow Agent 的功能测试助手，请用一句简短中文回答用户的问题。";
    }

    @Override
    protected String userPrompt(com.yomahub.liteflow.agent.context.LiteFlowAgentContext context) {
        Object reqData = getSlot().getChainReqData(getSlot().getChainId());
        return reqData == null ? "" : reqData.toString();
    }

    @Override
    protected int maxIterations() {
        return 3;
    }

    @Override
    protected boolean enableShellTool() {
        return false;
    }

    private static final class DeterministicModel implements Model {
        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            ContentBlock content = TextBlock.builder().text("deterministic reply").build();
            return Flux.just(ChatResponse.builder()
                    .content(List.of(content))
                    .finishReason("stop")
                    .build());
        }

        @Override
        public String getModelName() {
            return "build-model-escape-test";
        }
    }
}
