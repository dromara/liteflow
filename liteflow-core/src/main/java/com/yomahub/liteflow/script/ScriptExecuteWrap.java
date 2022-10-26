package com.yomahub.liteflow.script;

/**
 * script执行前的包装元参数
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class ScriptExecuteWrap {

    private int slotIndex;

    private String currChainId;

    private String nodeId;

    private String tag;

    private Object cmpData;

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    /**
     * @deprecated 请使用 {@link #getCurrChainId()} 
     */
    @Deprecated
    public String getCurrChainName() {
        return currChainId;
    }

    /**
     * 
     * @param currChainName
     * @deprecated 请使用{@link #setCurrChainId(String)}
     */
    public void setCurrChainName(String currChainName) {
        this.currChainId = currChainName;
    }
    
    public String getCurrChainId() {
        return currChainId;
    }

    public void setCurrChainId(String currChainId) {
        this.currChainId = currChainId;
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
