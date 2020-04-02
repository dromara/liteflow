package com.yomahub.liteflow.entity.flow;

import com.yomahub.liteflow.enums.ExecuteTypeEnum;

public interface Executable {

    void execute(Integer slotIndex) throws Exception;

    ExecuteTypeEnum getExecuteType();

    String getExecuteName();
}
