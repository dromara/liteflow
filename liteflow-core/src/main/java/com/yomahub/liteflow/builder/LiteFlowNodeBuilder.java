package com.yomahub.liteflow.builder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.NodeBuildException;
import com.yomahub.liteflow.exception.NullParamException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Node;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class LiteFlowNodeBuilder {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final Node node;

    public static LiteFlowNodeBuilder createNode() {
        return new LiteFlowNodeBuilder();
    }

    public static LiteFlowNodeBuilder createCommonNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.COMMON);
    }

    public static LiteFlowNodeBuilder createCommonCondNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.COMMON);
    }

    public static LiteFlowNodeBuilder createScriptNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.SCRIPT);
    }

    public static LiteFlowNodeBuilder createScriptCondNode() {
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
        if (StrUtil.isBlank(nodeId)) {
            return this;
        }
        this.node.setId(nodeId.trim());
        return this;
    }

    public LiteFlowNodeBuilder setName(String name) {
        if (StrUtil.isBlank(name)) {
            return this;
        }
        this.node.setName(name.trim());
        return this;
    }

    public LiteFlowNodeBuilder setClazz(String clazz) {
        if (StrUtil.isBlank(clazz)) {
            return this;
        }
        this.node.setClazz(clazz.trim());
        return this;
    }

    public LiteFlowNodeBuilder setClazz(Class<?> clazz) {
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
        if (StrUtil.isBlank(filePath)) {
            return this;
        }
        String script = ResourceUtil.readUtf8Str(StrUtil.format("classpath: {}", filePath.trim()));
        return setScript(script);
    }

    public void build() {
        checkBuild();
        try {
            if (this.node.getType().equals(NodeTypeEnum.COMMON)) {
                FlowBus.addCommonNode(this.node.getId(), this.node.getName(), this.node.getClazz());
            } else if (this.node.getType().equals(NodeTypeEnum.SCRIPT)) {
                FlowBus.addCommonScriptNode(this.node.getId(), this.node.getName(), this.node.getScript());
            } else if (this.node.getType().equals(NodeTypeEnum.COND_SCRIPT)) {
                FlowBus.addCondScriptNode(this.node.getId(), this.node.getName(), this.node.getScript());
            }
        } catch (Exception e) {
            String errMsg = StrUtil.format("An exception occurred while building the node[{}]", this.node.getId());
            LOG.error(errMsg, e);
            throw new NodeBuildException(errMsg);
        }
    }

    /**
     * build 前简单校验
     */
    private void checkBuild() {
        List<String> errorList = Lists.newArrayList();
        if (StrUtil.isBlank(this.node.getId())) {
            errorList.add("id is blank");
        }
        if (Objects.isNull(this.node.getType())) {
            errorList.add("type is null");
        }
        if (CollUtil.isNotEmpty(errorList)) {
            throw new NodeBuildException(CollUtil.join(errorList, ",", "[", "]"));
        }
    }
}
