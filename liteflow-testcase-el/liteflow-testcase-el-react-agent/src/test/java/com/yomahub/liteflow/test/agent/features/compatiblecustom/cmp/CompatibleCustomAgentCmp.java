package com.yomahub.liteflow.test.agent.features.compatiblecustom.cmp;

import com.yomahub.liteflow.test.agent.features.support.CompatibleCustomEchoAgentComponent;
import org.springframework.stereotype.Component;

/**
 * 通过 compatible-custom 配置解析构建的测试 Agent，执行时使用本地 Echo 模型。
 */
@Component("compatibleCustomAgent")
public class CompatibleCustomAgentCmp extends CompatibleCustomEchoAgentComponent {
}
