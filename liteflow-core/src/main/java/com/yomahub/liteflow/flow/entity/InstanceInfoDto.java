package com.yomahub.liteflow.flow.entity;

/**
 * InstanceInfo Dto
 * {"chainId":"chain1","nodeId":"a","instanceId":"XXXX","index":0}
 * @author jay li
 * @since 2.13.0
 */
public class InstanceInfoDto {

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
