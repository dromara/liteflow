package com.yomahub.liteflow.process.holder;

import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.core.AppContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点持有人（用于收集 Node 后统一注册）
 *
 * @author noear 2024/10/12 created
 */
public class SolonNodeHolder {
    /**
     * 作为 AppContext 附件（避免静态化）
     * */
    public static SolonNodeHolder of(AppContext context) {
        return context.attachOf(SolonNodeHolder.class, SolonNodeHolder::new);
    }

    private SolonNodeHolder() {

    }

    private Map<String, NodeComponent> nodeMap = new HashMap<>();

    public void put(String nodeId, NodeComponent nodeComponent) {
        this.nodeMap.put(nodeId, nodeComponent);
    }

    public Map<String, NodeComponent> getNodeMap() {
        return nodeMap;
    }
}
