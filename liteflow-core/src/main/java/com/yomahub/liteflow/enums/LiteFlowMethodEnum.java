package com.yomahub.liteflow.enums;

public enum LiteFlowMethodEnum {
    PROCESS("process"),
    PROCESS_SWITCH("processSwitch"),
    IS_ACCESS("isAccess"),

    IS_END("isEnd"),
    IS_CONTINUE_ON_ERROR("isContinueOnError"),

    GET_NODE_EXECUTOR_CLASS("getNodeExecutorClass"),

    ON_SUCCESS("onSuccess"),

    ON_ERROR("onError"),

    BEFORE_PROCESS("beforeProcess"),

    AFTER_PROCESS("afterProcess");

    private String methodName;

    LiteFlowMethodEnum(String methodName){
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
