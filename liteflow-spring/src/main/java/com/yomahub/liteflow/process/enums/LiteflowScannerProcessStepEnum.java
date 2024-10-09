package com.yomahub.liteflow.process.enums;

/**
 * spring bean finder 枚举类型
 *
 * @author tkc
 * @since 2.12.4
 */
public enum LiteflowScannerProcessStepEnum {
    DECL_WARP_BEAN("声明式组件", Integer.MIN_VALUE),
    NODE_CMP_BEAN("普通组件", Integer.MIN_VALUE + 10),
    CMP_AROUND_ASPECT_BEAN("组件Aop的实现类", Integer.MIN_VALUE + 20),
    SCRIPT_BEAN("@ScriptBean修饰的类", Integer.MIN_VALUE + 30),
    SCRIPT_METHOD_BEAN("@ScriptMethod修饰的类", Integer.MIN_VALUE + 40),
    DATA_BASE_CONNECT_BEAN("sql 插件数据库连接获取", Integer.MIN_VALUE + 50),
    ;
    private final String desc;
    private final Integer priority;

    LiteflowScannerProcessStepEnum(String desc, Integer priority) {
        this.desc = desc;
        this.priority = priority;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getDesc() {
        return desc;
    }
}
