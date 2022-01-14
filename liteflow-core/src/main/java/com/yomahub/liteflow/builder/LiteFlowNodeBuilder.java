package com.yomahub.liteflow.builder;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.entity.flow.Node;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.NodeBuildException;
import com.yomahub.liteflow.flow.FlowBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiteFlowNodeBuilder {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final Node node;

    public static LiteFlowNodeBuilder createNode() {
        return new LiteFlowNodeBuilder();
    }

    public LiteFlowNodeBuilder() {
        this.node = new Node();
    }

    public LiteFlowNodeBuilder setId(String nodeId) {
        this.node.setId(nodeId);
        return this;
    }

    public LiteFlowNodeBuilder setName(String name) {
        this.node.setName(name);
        return this;
    }

    public LiteFlowNodeBuilder setClazz(String clazz) {
        this.node.setClazz(clazz);
        return this;
    }

    public LiteFlowNodeBuilder setType(NodeTypeEnum type) {
        this.node.setType(type);
        return this;
    }

    public LiteFlowNodeBuilder setScript(String script) {
        this.node.setScript(script);
        return this;
    }

    public LiteFlowNodeBuilder setFile(String filePath) {
        if (StrUtil.isBlank(filePath)){
            return this;
        }
        String script = ResourceUtil.readUtf8Str(StrUtil.format("classpath: {}", filePath));
        return setScript(script);
    }

    public void build() {
        try {
            if (this.node.getType().equals(NodeTypeEnum.COMMON)) {
                FlowBus.addCommonNode(this.node.getId(), this.node.getName(), this.node.getClazz());
            } else if (this.node.getType().equals(NodeTypeEnum.SCRIPT)){
                FlowBus.addCommonScriptNode(this.node.getId(), this.node.getName(), this.node.getScript());
            } else if (this.node.getType().equals(NodeTypeEnum.COND_SCRIPT)){
                FlowBus.addCondScriptNode(this.node.getId(), this.node.getName(), this.node.getScript());
            }
        } catch (Exception e) {
            String errMsg = StrUtil.format("An exception occurred while building the node[{}]", this.node.getId());
            LOG.error(errMsg, e);
            throw new NodeBuildException(errMsg);
        }
    }
}
