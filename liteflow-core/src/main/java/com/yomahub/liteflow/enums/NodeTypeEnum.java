package com.yomahub.liteflow.enums;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.*;

import java.util.Arrays;
import java.util.Objects;

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

    private Class<? extends NodeComponent> mappingClazz;

    NodeTypeEnum(String code, String name, boolean isScript, Class<? extends NodeComponent> mappingClazz) {
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

    public Class<? extends NodeComponent> getMappingClazz() {
        return mappingClazz;
    }

    public void setMappingClazz(Class<? extends NodeComponent> mappingClazz) {
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

    public static NodeTypeEnum guessTypeBySuperClazz(Class<?> clazz){
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

    public static NodeTypeEnum guessType(Class<?> clazz){
        NodeTypeEnum nodeType = guessTypeBySuperClazz(clazz);
        if (nodeType == null){
            //再尝试声明式组件这部分的推断
            LiteflowMethod liteflowMethod = Arrays.stream(clazz.getDeclaredMethods()).map(
                    method -> AnnotationUtil.getAnnotation(method, LiteflowMethod.class)
            ).filter(Objects::nonNull).filter(lfMethod -> lfMethod.value().isMainMethod()).findFirst().orElse(null);

            if (liteflowMethod != null) {
                nodeType = liteflowMethod.nodeType();
            }
        }
        return nodeType;
    }
}
