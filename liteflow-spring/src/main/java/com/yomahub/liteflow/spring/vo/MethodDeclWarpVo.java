package com.yomahub.liteflow.spring.vo;

import com.yomahub.liteflow.core.NodeComponent;

public class MethodDeclWarpVo {

    private String nodeId;

    private NodeComponent nodeComponent;

    public MethodDeclWarpVo(String nodeId, NodeComponent nodeComponent) {
        this.nodeId = nodeId;
        this.nodeComponent = nodeComponent;
    }
}
