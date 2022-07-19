package com.yomahub.liteflow.flow.element;

import com.yomahub.liteflow.enums.ExecuteTypeEnum;

/**
 * 可执行器接口
 * 目前实现这个接口的有3个，Chain，Condition，Node
 *
 * @author Bryan.Zhang
 */
public interface Executable{

    void execute(Integer slotIndex) throws Exception;

    default boolean isAccess(Integer slotIndex) throws Exception{
        return true;
    }

    ExecuteTypeEnum getExecuteType();

    String getExecuteName();

    default void setCurrChainName(String currentChainName){

    }
}
