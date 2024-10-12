package com.yomahub.liteflow.process.holder;

import org.noear.solon.core.AppContext;

import java.util.HashSet;
import java.util.Set;

/**
 * 节点持有人（用于收集 Node 后统一注册）
 *
 * @author noear 2024/10/12 created
 */
public class SolonNodeIdHolder {
    /**
     * 作为 AppContext 附件（避免静态化）
     * */
    public static SolonNodeIdHolder of(AppContext context) {
        return context.attachOf(SolonNodeIdHolder.class, SolonNodeIdHolder::new);
    }

    private SolonNodeIdHolder() {

    }

    private Set<String> nodeIdSet = new HashSet<>();

    public void add(String nodeId) {
        this.nodeIdSet.add(nodeId);
    }

    public Set<String> getNodeIdSet() {
        return nodeIdSet;
    }
}
