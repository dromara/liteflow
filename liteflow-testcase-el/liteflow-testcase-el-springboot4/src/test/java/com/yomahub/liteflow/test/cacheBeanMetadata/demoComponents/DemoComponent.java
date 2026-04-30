package com.yomahub.liteflow.test.cacheBeanMetadata.demoComponents;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

@LiteflowComponent
public class DemoComponent {
    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "demo", nodeName = "demo组件", nodeType = NodeTypeEnum.COMMON)
    public void process(NodeComponent demoNode) throws Exception {
        System.out.println("当前执行 processA 方法, demoNode.getClass is " + demoNode.getClass() + ", demoNode.getSuperClass is " + demoNode.getClass().getSuperclass());
    }
}
