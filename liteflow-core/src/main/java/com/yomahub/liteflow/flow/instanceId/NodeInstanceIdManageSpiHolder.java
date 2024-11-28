package com.yomahub.liteflow.flow.instanceId;

import cn.hutool.core.util.ObjectUtil;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author Jay li
 * @since 2.13.0
 */
public class NodeInstanceIdManageSpiHolder {
    private NodeInstanceIdManageSpi nodeInstanceIdManageSpi;

    private static final NodeInstanceIdManageSpiHolder INSTANCE = new NodeInstanceIdManageSpiHolder();

    public static void init() {
        ServiceLoader<NodeInstanceIdManageSpi> loader = ServiceLoader.load(NodeInstanceIdManageSpi.class);
        Iterator<NodeInstanceIdManageSpi> iterator = loader.iterator();
        if (iterator.hasNext()) {
            INSTANCE.setNodeInstanceIdManageSpi(iterator.next());
        } else {
            INSTANCE.setNodeInstanceIdManageSpi(new DefaultNodeInstanceIdManageSpiImpl());
        }
    }

    public static NodeInstanceIdManageSpiHolder getInstance() {
        return INSTANCE;
    }

    public NodeInstanceIdManageSpi getNodeInstanceIdManageSpi() {
        if (ObjectUtil.isNull(nodeInstanceIdManageSpi)) {
            init();
        }
        return nodeInstanceIdManageSpi;
    }

    public void setNodeInstanceIdManageSpi(NodeInstanceIdManageSpi nodeInstanceIdManageSpi) {
        this.nodeInstanceIdManageSpi = nodeInstanceIdManageSpi;
    }

}
