package com.yomahub.liteflow.core;

import com.yomahub.liteflow.enums.NodeTypeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * 脚本接口
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public interface ScriptComponent {
    /**
     * 用于维护脚本类型和脚本 cmp 的映射关系
     */
    Map<NodeTypeEnum, Class<?>> ScriptComponentClassMap = new HashMap<NodeTypeEnum, Class<?>>() {{
        put(NodeTypeEnum.SCRIPT, ScriptCommonComponent.class);
        put(NodeTypeEnum.SWITCH_SCRIPT, ScriptSwitchComponent.class);
        put(NodeTypeEnum.IF_SCRIPT, ScriptIfComponent.class);
        put(NodeTypeEnum.FOR_SCRIPT, ScriptForComponent.class);
        put(NodeTypeEnum.WHILE_SCRIPT, ScriptWhileComponent.class);
        put(NodeTypeEnum.BREAK_SCRIPT, ScriptBreakComponent.class);
    }};


    /**
     * 加载脚本
     * @param script
     */
    void loadScript(String script);
}
