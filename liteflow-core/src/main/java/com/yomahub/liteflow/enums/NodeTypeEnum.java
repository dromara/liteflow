package com.yomahub.liteflow.enums;

import com.yomahub.liteflow.core.*;

/**
 * 节点类型枚举
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public enum NodeTypeEnum {

    COMMON("common","普通", false, NodeComponent.class),

    SWITCH("switch", "选择", false, NodeSwitchComponent.class),

    IF("if", "条件", false, NodeIfComponent.class),

    FOR("for","循环次数", false, NodeForComponent.class),

    WHILE("while", "循环条件", false, NodeWhileComponent.class),

    BREAK("break", "循环跳出", false, NodeBreakComponent.class),
    SCRIPT("script","脚本", true, ScriptCommonComponent.class),

    SWITCH_SCRIPT("switch_script", "选择脚本", true, ScriptSwitchComponent.class),

    IF_SCRIPT("if_script", "条件脚本", true, ScriptIfComponent.class),

    FOR_SCRIPT("for_script", "循环次数脚本", true, ScriptForComponent.class),

    WHILE_SCRIPT("while_script", "循环条件脚本", true, ScriptWhileComponent.class),

    BREAK_SCRIPT("break_script", "循环跳出脚本", true, ScriptBreakComponent.class)
    ;
    private String code;
    private String name;

    private boolean isScript;

    private Class<?> mappingClazz;

    NodeTypeEnum(String code, String name, boolean isScript, Class<?> mappingClazz) {
        this.code = code;
        this.name = name;
        this.isScript = isScript;
        this.mappingClazz = mappingClazz;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isScript() {
        return isScript;
    }

    public void setScript(boolean script) {
        isScript = script;
    }

    public Class<?> getMappingClazz() {
        return mappingClazz;
    }

    public void setMappingClazz(Class<?> mappingClazz) {
        this.mappingClazz = mappingClazz;
    }

    public static NodeTypeEnum getEnumByCode(String code) {
        for (NodeTypeEnum e : NodeTypeEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

    public static NodeTypeEnum guessTypeByClazz(Class<?> clazz){
        Class<?> superClazz = clazz;
        while(true){
            superClazz = superClazz.getSuperclass();
            if (superClazz.getPackage().getName().startsWith("com.yomahub.liteflow.core")){
                break;
            }
            if(superClazz.equals(Object.class)){
                return null;
            }
        }

        for (NodeTypeEnum e : NodeTypeEnum.values()) {
            if (e.getMappingClazz().equals(superClazz)) {
                return e;
            }
        }
        return null;
    }
}
