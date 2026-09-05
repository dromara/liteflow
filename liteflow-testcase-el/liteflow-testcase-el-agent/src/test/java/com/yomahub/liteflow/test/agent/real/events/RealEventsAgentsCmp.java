package com.yomahub.liteflow.test.agent.real.events;

import com.yomahub.liteflow.agent.component.AgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * guide §6 流式事件与 ChatUsage 的真实模型组件。
 */
final class RealEventsAgentsCmp {

    private RealEventsAgentsCmp() {
    }

    /** handleReply 中捕获的 ChatUsage 快照（§6.2）。 */
    static final List<ChatUsage> CAPTURED_USAGES = new CopyOnWriteArrayList<>();

    static void reset() {
        CAPTURED_USAGES.clear();
    }

    abstract static class AbstractEventsAgent extends AgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected String systemPrompt() {
            return "你是事件测试助手，请用一句简短中文回答。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            return reqData == null ? "" : reqData.toString();
        }

        @Override
        protected void handleReply(Msg reply, LiteFlowAgentContext context) {
            CAPTURED_USAGES.add(context.getChatUsage());
            super.handleReply(reply, context);
        }
    }

    @Component("realEventsAgent")
    static class EventsAgentCmp extends AbstractEventsAgent {
    }

    /**
     * LOG_AND_CONTINUE 专用组件：事件桥在 runtime 构建期固化失败模式，
     * 因此该组件必须在本用例设置配置之后才首次执行。
     */
    @Component("realEventsTolerantAgent")
    static class EventsTolerantAgentCmp extends AbstractEventsAgent {
    }

    /** FAIL_FAST 专用组件，避免与 LOG_AND_CONTINUE 用例的构建顺序耦合。 */
    @Component("realEventsStrictAgent")
    static class EventsStrictAgentCmp extends AbstractEventsAgent {
    }

    @Component("realEventsToolAgent")
    static class EventsToolAgentCmp extends AbstractEventsAgent {

        @Override
        protected List<Object> tools() {
            return List.of(new WeatherTool());
        }

        @Override
        protected String systemPrompt() {
            return "你是天气助手。必须先调用工具查询真实数据，再用一句中文回答。";
        }

        @Override
        protected int maxIterations() {
            return 6;
        }
    }

    static class WeatherTool {

        @Tool(name = "get_weather", description = "查询指定城市的天气")
        public String getWeather(
                @ToolParam(name = "city", description = "城市名") String city) {
            return city + " 今天晴，气温 26 摄氏度，空气质量优。";
        }
    }
}
