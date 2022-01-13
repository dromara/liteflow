package com.yomahub.liteflow.builder;

import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.flow.Node;
import com.yomahub.liteflow.enums.NodeTypeEnum;

public class LiteFlowNodeBuilder {

    private final Node node;

    public static LiteFlowNodeBuilder createNode(){
        return new LiteFlowNodeBuilder();
    }

    public LiteFlowNodeBuilder() {
        this.node = new Node();
    }

    public LiteFlowNodeBuilder setId(String nodeId){
        this.node.setId(nodeId);
        return this;
    }

    public LiteFlowNodeBuilder setName(String name){
        this.node.setName(name);
        return this;
    }

    public LiteFlowNodeBuilder setType(NodeTypeEnum type){
        this.node.setType(type);
        return this;
    }

}
