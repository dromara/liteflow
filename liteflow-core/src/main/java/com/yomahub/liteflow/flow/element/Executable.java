package com.yomahub.liteflow.flow.element;

import com.yomahub.liteflow.enums.ExecuteTypeEnum;

/**
 * 可执行器接口
 * 目前实现这个接口的有2个，node和chain
 *
 * @author Bryan.Zhang
 */
public interface Executable{

    void execute(Integer slotIndex) throws Exception;

    ExecuteTypeEnum getExecuteType();

    String getExecuteName();
}
