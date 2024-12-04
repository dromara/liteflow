package com.yomahub.liteflow.flow.entity;

/**
 * sInstanceId
 *
 * @author jay li
 * @since 2.13.0
 */
public class InstanceIdDto {
    // a_XXX_0
    // {"chainId":"chain1","nodeId":"a","instanceId":"XXXX","index":0},
    private String chainId;

    private String nodeId;

    private String instanceId;

    private Integer index;

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }
}
