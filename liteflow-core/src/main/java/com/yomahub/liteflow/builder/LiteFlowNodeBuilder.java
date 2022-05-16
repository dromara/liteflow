package com.yomahub.liteflow.builder;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.flow.Node;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.NodeBuildException;
import com.yomahub.liteflow.exception.NullParamException;
import com.yomahub.liteflow.flow.FlowBus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiteFlowNodeBuilder {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final Node node;

    public static LiteFlowNodeBuilder createNode() {
        return new LiteFlowNodeBuilder();
    }

    public static LiteFlowNodeBuilder createCommonNode(){
        return new LiteFlowNodeBuilder(NodeTypeEnum.COMMON);
    }

    public static LiteFlowNodeBuilder createCommonCondNode(){
        return new LiteFlowNodeBuilder(NodeTypeEnum.COMMON);
    }

    public static LiteFlowNodeBuilder createScriptNode(){
        return new LiteFlowNodeBuilder(NodeTypeEnum.SCRIPT);
    }

    public static LiteFlowNodeBuilder createScriptCondNode(){
        return new LiteFlowNodeBuilder(NodeTypeEnum.COND_SCRIPT);
    }

    public LiteFlowNodeBuilder() {
        this.node = new Node();
    }

    public LiteFlowNodeBuilder(NodeTypeEnum type) {
        this.node = new Node();
        this.node.setType(type);
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

    public LiteFlowNodeBuilder setClazz(Class<?> clazz){
        assert clazz != null;
        setClazz(clazz.getName());
        return this;
    }

    // 设置节点组件的class
    public LiteFlowNodeBuilder setNodeComponentClazz(Class<? extends NodeComponent> nodeComponentClass) {
        assert nodeComponentClass != null;
        setClazz(nodeComponentClass.getName());
        return this;
    }

    public LiteFlowNodeBuilder setType(NodeTypeEnum type) {
        this.node.setType(type);
        return this;
    }

    // 设置类型的编码
    public LiteFlowNodeBuilder setTypeCode(String nodeTypeCode) {
       if (StringUtils.isBlank(nodeTypeCode)) {
            throw new NullParamException("nodeTypeCode is blank");
        }
        NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(nodeTypeCode);
        if (ObjectUtil.isNull(nodeTypeEnum)) {
            throw new NullParamException(StrUtil.format("nodeTypeCode[{}] is error", nodeTypeCode));
        }
        setType(nodeTypeEnum);
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
