package com.yomahub.liteflow.script;

/**
 * script执行前的包装元参数
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class ScriptExecuteWrap {

    private int slotIndex;

    private String currChainName;

    private String nodeId;

    private String tag;

    private Object cmpData;

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public String getCurrChainName() {
        return currChainName;
    }

    public void setCurrChainName(String currChainName) {
        this.currChainName = currChainName;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Object getCmpData() {
        return cmpData;
    }

    public void setCmpData(Object cmpData) {
        this.cmpData = cmpData;
    }
}
