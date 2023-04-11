package com.yomahub.liteflow.script;

/**
 * script执行前的包装元参数
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class ScriptExecuteWrap {

	private int slotIndex;

	private String currChainId;

	private String nodeId;

	private String tag;

	private Object cmpData;

	private Integer loopIndex;

	private Object loopObject;

	public int getSlotIndex() {
		return slotIndex;
	}

	public void setSlotIndex(int slotIndex) {
		this.slotIndex = slotIndex;
	}

	/**
	 * @deprecated 请使用 {@link #getCurrChainId()}
	 * @return String
	 */
	@Deprecated
	public String getCurrChainName() {
		return currChainId;
	}

	/**
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

	public Integer getLoopIndex() {
		return loopIndex;
	}

	public void setLoopIndex(Integer loopIndex) {
		this.loopIndex = loopIndex;
	}

	public Object getLoopObject() {
		return loopObject;
	}

	public void setLoopObject(Object loopObject) {
		this.loopObject = loopObject;
	}
}
