package com.yomahub.liteflow.builder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.NodeBuildException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LiteFlowNodeBuilder {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    /**
     * 用于维护不同类型 node 的处理逻辑
     */
    private static final Map<NodeTypeEnum, BiConsumer<Node, NodeTypeEnum>> NodeBuildConsumerMap = new HashMap<NodeTypeEnum, BiConsumer<Node, NodeTypeEnum>>() {{
        // 用于处理普通 node
        put(NodeTypeEnum.COMMON, (_node, nodeType) -> FlowBus.addNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));
        put(NodeTypeEnum.SWITCH, (_node, nodeType) -> FlowBus.addNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));
        put(NodeTypeEnum.IF, (_node, nodeType) -> FlowBus.addNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));
        put(NodeTypeEnum.FOR, (_node, nodeType) -> FlowBus.addNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));
        put(NodeTypeEnum.WHILE, (_node, nodeType) -> FlowBus.addNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));
        put(NodeTypeEnum.BREAK, (_node, nodeType) -> FlowBus.addNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));

        // 用于处理脚本 node
        put(NodeTypeEnum.SCRIPT, (_node, nodeType) -> FlowBus.addScriptNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));
        put(NodeTypeEnum.SWITCH_SCRIPT, (_node, nodeType) -> FlowBus.addScriptNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));
        put(NodeTypeEnum.IF_SCRIPT, (_node, nodeType) -> FlowBus.addScriptNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));
        put(NodeTypeEnum.FOR_SCRIPT, (_node, nodeType) -> FlowBus.addScriptNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));
        put(NodeTypeEnum.WHILE_SCRIPT, (_node, nodeType) -> FlowBus.addScriptNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));
        put(NodeTypeEnum.BREAK_SCRIPT, (_node, nodeType) -> FlowBus.addScriptNode(_node.getId(), _node.getName(), nodeType, _node.getClazz()));
    }};

    private final Node node;

    public static LiteFlowNodeBuilder createNode() {
        return new LiteFlowNodeBuilder();
    }

    public static LiteFlowNodeBuilder createCommonNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.COMMON);
    }

    public static LiteFlowNodeBuilder createSwitchNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.SWITCH);
    }

    public static LiteFlowNodeBuilder createIfNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.IF);
    }

    public static LiteFlowNodeBuilder createForNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.FOR);
    }

    public static LiteFlowNodeBuilder createWhileNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.WHILE);
    }

    public static LiteFlowNodeBuilder createBreakNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.BREAK);
    }

    public static LiteFlowNodeBuilder createScriptNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.SCRIPT);
    }

    public static LiteFlowNodeBuilder createScriptSwitchNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.SWITCH_SCRIPT);
    }

    public static LiteFlowNodeBuilder createScriptIfNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.IF_SCRIPT);
    }

    public static LiteFlowNodeBuilder createScriptForNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.FOR_SCRIPT);
    }

    public static LiteFlowNodeBuilder createScriptWhileNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.WHILE_SCRIPT);
    }

    public static LiteFlowNodeBuilder createScriptBreakNode() {
        return new LiteFlowNodeBuilder(NodeTypeEnum.BREAK_SCRIPT);
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

    public LiteFlowNodeBuilder setType(NodeTypeEnum type) {
        this.node.setType(type);
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
            for (Map.Entry<NodeTypeEnum, BiConsumer<Node, NodeTypeEnum>> entry : NodeBuildConsumerMap.entrySet()) {
                NodeTypeEnum nodeType = entry.getKey();
                if (nodeType == this.node.getType()) {
                    entry.getValue().accept(this.node, nodeType);
                    return;
                }
            }
        } catch (Exception e) {
            String errMsg = StrUtil.format("An exception occurred while building the node[{}],{}", this.node.getId(), e.getMessage());
            LOG.error(errMsg, e);
            throw new NodeBuildException(errMsg);
        }
    }

    /**
     * build 前简单校验
     */
    private void checkBuild() {
        List<String> errorList = new ArrayList<>();
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
