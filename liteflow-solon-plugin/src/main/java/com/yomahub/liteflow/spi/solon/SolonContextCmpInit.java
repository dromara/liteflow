package com.yomahub.liteflow.spi.solon;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.solon.integration.XPluginImpl;
import com.yomahub.liteflow.spi.ContextCmpInit;

import java.util.Map;


/**
 * Solon 环境容器上下文组件初始化实现
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class SolonContextCmpInit implements ContextCmpInit {
    @Override
    public void initCmp() {
        for (Map.Entry<String, NodeComponent> componentEntry : XPluginImpl.nodeComponentMap.entrySet()) {
            if (!FlowBus.containNode(componentEntry.getKey())) {
                FlowBus.addSpringScanNode(componentEntry.getKey(), componentEntry.getValue());
            }
        }
    }

    @Override
    public int priority() {
        return 1;
    }
}
