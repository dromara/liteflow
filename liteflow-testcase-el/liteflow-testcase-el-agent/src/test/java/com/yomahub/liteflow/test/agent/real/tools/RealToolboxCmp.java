package com.yomahub.liteflow.test.agent.real.tools;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * guide §4 工具 的真实模型组件集合。
 */
public final class RealToolboxCmp {

    private RealToolboxCmp() {
    }

    /** 记录工具是否被真实模型调用过。 */
    public static final class ToolLedger {
        static final List<String> CALLS = new CopyOnWriteArrayList<>();
        static final AtomicInteger ORDER_HITS = new AtomicInteger();

        static void reset() {
            CALLS.clear();
            ORDER_HITS.set(0);
        }

        public static List<String> calls() {
            return List.copyOf(CALLS);
        }

        static void record(String tool, String detail) {
            CALLS.add(tool + ":" + detail);
        }
    }

    /** §4.1 普通 Java 对象工具：@Tool 注解方法。 */
    public static class OrderTool {

        @Tool(name = "query_order", description = "按订单号查询订单状态")
        public String queryOrder(
                @ToolParam(name = "orderId", description = "订单号，例如 10001") String orderId) {
            ToolLedger.record("query_order", orderId);
            ToolLedger.ORDER_HITS.incrementAndGet();
            return "订单 " + orderId + " 已发货，预计 3 天送达";
        }
    }

    abstract static class AbstractToolAgent extends HarnessAgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected String systemPrompt() {
            return "你是订单客服助手。回答用户问题时必须优先调用可用的查询工具获取事实，"
                    + "再基于工具返回内容用一句中文作答，不要编造。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            return reqData == null ? "" : reqData.toString();
        }

        @Override
        protected int maxIterations() {
            return 6;
        }
    }

    /** §4.1 组件内联工具。 */
    @Component("realOrderAgent")
    public static class OrderAgentCmp extends AbstractToolAgent {
        @Override
        protected List<Object> tools() {
            return List.of(new OrderTool());
        }
    }

    /** §4.2 Spring bean 工具：GreetingService 注入。 */
    @org.springframework.stereotype.Component
    public static class GreetingService {
        public String hello(String name) {
            return "你好，" + name + "！欢迎来到 LiteFlow 真实工具测试。";
        }
    }

    @org.springframework.stereotype.Component
    public static class GreetingTool {

        private final GreetingService greetingService;

        public GreetingTool(GreetingService greetingService) {
            this.greetingService = greetingService;
        }

        @Tool(name = "greet_user", description = "按用户姓名生成欢迎语")
        public String greetUser(
                @ToolParam(name = "name", description = "用户姓名") String name) {
            String greeting = greetingService.hello(name);
            ToolLedger.record("greet_user", name);
            return greeting;
        }
    }

    /** §4.2 组件：返回注入的 bean 工具。 */
    @Component("realBeanToolAgent")
    public static class BeanToolAgentCmp extends AbstractToolAgent {

        private final GreetingTool greetingTool;

        public BeanToolAgentCmp(GreetingTool greetingTool) {
            this.greetingTool = greetingTool;
        }

        @Override
        protected List<Object> tools() {
            return List.of(greetingTool);
        }
    }

    /** §4.3 内置文件工具：view/list/write/insert。 */
    @Component("realFileToolsAgent")
    public static class FileToolsAgentCmp extends AbstractToolAgent {

        @Override
        protected String systemPrompt() {
            return "你是文件操作助手。请严格按用户指令使用文件工具完成任务，"
                    + "完成后用一句中文报告结果。";
        }
    }

    /** §4.3 内置 shell 工具（白名单模式）。 */
    @Component("realShellToolAgent")
    public static class ShellToolAgentCmp extends AbstractToolAgent {

        @Override
        protected boolean enableShellTool() {
            return true;
        }

        @Override
        protected String systemPrompt() {
            return "你是命令行助手。请严格按用户指令使用 execute_shell_command 工具执行命令，"
                    + "并把命令输出原样转告用户。";
        }
    }
}
