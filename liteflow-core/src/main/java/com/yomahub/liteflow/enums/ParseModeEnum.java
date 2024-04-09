package com.yomahub.liteflow.enums;

/**
 * 解析模式
 * PARSE_ALL_ON_START 启动时解析所有的规则
 * PARSE_ALL_ON_FIRST_EXEC 第一次执行链路时解析所有的规则
 * PARSE_ONE_ON_FIRST_EXEC 第一次执行相关链路时解析当前的规则
 *
 * @author Bryan.Zhang
 * @since 2.12.0
 */
public enum ParseModeEnum {

    PARSE_ALL_ON_START,
    PARSE_ALL_ON_FIRST_EXEC,
    PARSE_ONE_ON_FIRST_EXEC
}
