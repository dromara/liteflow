package com.yomahub.liteflow.enums;

public enum LiteFlowMethodEnum {

	PROCESS("process", true),
	PROCESS_SWITCH("processSwitch", true),
	PROCESS_BOOLEAN("processBoolean", true),
	PROCESS_FOR("processFor", true),

	PROCESS_ITERATOR("processIterator", true),

	IS_ACCESS("isAccess", false),

	IS_END("isEnd", false),
	IS_CONTINUE_ON_ERROR("isContinueOnError", false),

	GET_NODE_EXECUTOR_CLASS("getNodeExecutorClass", false),

	ON_SUCCESS("onSuccess", false),

	ON_ERROR("onError", false),

	BEFORE_PROCESS("beforeProcess", false),

	AFTER_PROCESS("afterProcess", false),

	GET_DISPLAY_NAME("getDisplayName", false),

	ROLLBACK("rollback", false)
	;

	private String methodName;

	private boolean isMainMethod;

	LiteFlowMethodEnum(String methodName, boolean isMainMethod) {
		this.methodName = methodName;
		this.isMainMethod = isMainMethod;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public boolean isMainMethod() {
		return isMainMethod;
	}

	public void setMainMethod(boolean mainMethod) {
		isMainMethod = mainMethod;
	}

}
