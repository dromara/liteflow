package com.yomahub.liteflow.test.agent.real.harness.docker;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import io.agentscope.core.permission.PermissionContextState;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * guide §12 DOCKER 沙箱后端的真实模型组件：模型驱动命令在 Docker 沙箱内执行。
 */
final class RealDockerSandboxCmp {

    private RealDockerSandboxCmp() {
    }

    @Component("realDockerSandboxAgent")
    static class DockerSandboxAgentCmp extends HarnessAgentComponent {

        @Override
        protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() {
            return RealAgentTestBase.realModel();
        }

        @Override
        protected PermissionContextState permissionContext() {
            PermissionContextState.Builder builder = PermissionContextState.builder();
            for (String tool : List.of("execute", "write_file", "read_file")) {
                builder.addAllowRule(tool, new io.agentscope.core.permission.PermissionRule(
                        tool, null, io.agentscope.core.permission.PermissionBehavior.ALLOW,
                        "real docker sandbox test"));
            }
            return builder.build();
        }

        @Override
        protected String systemPrompt() {
            return "你是沙箱测试助手。请严格使用 execute 工具在沙箱中执行命令并汇报输出。";
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
}
