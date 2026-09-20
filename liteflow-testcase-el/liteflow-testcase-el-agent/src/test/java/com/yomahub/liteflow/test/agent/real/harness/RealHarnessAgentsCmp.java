package com.yomahub.liteflow.test.agent.real.harness;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.memory.compaction.ToolResultEvictionConfig;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * guide §12 Harness 模块的真实模型组件：
 * 文件系统、上下文压缩、长期记忆、工具结果淘汰、子代理与计划模式。
 */
final class RealHarnessAgentsCmp {

    private RealHarnessAgentsCmp() {
    }

    abstract static class AbstractRealHarnessAgent extends HarnessAgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected String systemPrompt() {
            return "你是 Harness 测试助手，请严格按用户指令行动并用简短中文汇报。";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            Object reqData = getSlot().getChainReqData(getSlot().getChainId());
            return reqData == null ? "" : reqData.toString();
        }

        @Override
        protected int maxIterations() {
            return 8;
        }
    }

    /** §12 GUARDED_LOCAL：模型经文件系统工具写入 workspace。 */
    @Component("realHarnessFileAgent")
    static class HarnessFileAgentCmp extends AbstractRealHarnessAgent {

        @Override
        protected String systemPrompt() {
            return "你是文件助手。请严格使用可用的文件工具完成任务，完成后一句话汇报。";
        }
    }

    /** §12.1 上下文压缩：低阈值触发。 */
    @Component("realCompactionAgent")
    static class CompactionAgentCmp extends AbstractRealHarnessAgent {

        @Override
        protected CompactionConfig compactionConfig() {
            return CompactionConfig.builder()
                    .triggerMessages(6)
                    .keepMessages(4)
                    .keepTokens(1)
                    .build();
        }
    }

    /** §12.2 长期记忆：用真实模型抽取记忆。 */
    @Component("realMemoryAgent")
    static class MemoryAgentCmp extends AbstractRealHarnessAgent {

        @Override
        protected MemoryConfig memoryConfig() {
            return MemoryConfig.builder()
                    .model(RealAgentTestBase.realModel().resolve(agentConfig()))
                    .build();
        }
    }

    /** §12.3 工具结果淘汰：超大工具返回只保留预览。 */
    @Component("realEvictionAgent")
    static class EvictionAgentCmp extends AbstractRealHarnessAgent {

        @Override
        protected ToolResultEvictionConfig toolResultEvictionConfig() {
            return ToolResultEvictionConfig.builder()
                    .maxResultChars(600)
                    .previewChars(120)
                    .build();
        }

        @Override
        protected List<Object> tools() {
            return List.of(new BigResultTool());
        }

        @Override
        protected String systemPrompt() {
            return "你是淘汰测试助手。必须先调用工具，然后只回答：工具结果的开头关键词和总长度。";
        }
    }

    static class BigResultTool {

        @Tool(name = "fetch_big_report", description = "获取一份超长的报告文本")
        public String fetchBigReport(
                @ToolParam(name = "title", description = "报告标题") String title) {
            StringBuilder builder = new StringBuilder();
            builder.append("REPORT-HEADER-BIG-DATA-");
            for (int i = 0; i < 40_000; i++) {
                builder.append('x');
            }
            return builder.toString();
        }
    }

    /** §12.4 子代理 + 计划模式。 */
    @Component("realSubagentPlanAgent")
    static class SubagentPlanAgentCmp extends AbstractRealHarnessAgent {

        @Override
        protected List<SubagentDeclaration> subagents() {
            return List.of(SubagentDeclaration.builder()
                    .name("researcher")
                    .description("负责检索资料并汇总要点，一次输出要点清单")
                    .inlineAgentsBody("你是资料检索专员，只输出要点清单。")
                    .build());
        }

        @Override
        protected boolean enablePlanMode() {
            return true;
        }

        @Override
        protected String systemPrompt() {
            return "你是编排测试助手。面对多步骤任务：先（如可用）进入计划模式列出计划，"
                    + "再把资料检索工作委派给 researcher 子代理，最后汇总。";
        }

        @Override
        protected int maxIterations() {
            return 16;
        }
    }

    /** 关闭全部可选能力的基础 Agent（对照：不启用 harness 增强也可运行）。 */
    @Component("realHarnessMinimalAgent")
    static class HarnessMinimalAgentCmp extends AbstractRealHarnessAgent {

        @Override
        protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            return builder
                    .disableCompaction()
                    .disableToolResultEviction()
                    .disableMemoryTools()
                    .disableMemoryHooks()
                    .disableWorkspaceContext()
                    .disableAtPathExpansion()
                    .disableDefaultWorkspaceSkills()
                    .disableDynamicSkills()
                    .disableToolsConfig()
                    .disableFilesystemTools();
        }
    }
}
