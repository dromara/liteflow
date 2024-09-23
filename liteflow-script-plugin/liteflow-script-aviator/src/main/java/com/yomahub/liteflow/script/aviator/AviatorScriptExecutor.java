package com.yomahub.liteflow.script.aviator;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.jsr223.JSR223ScriptExecutor;

public class AviatorScriptExecutor extends JSR223ScriptExecutor {

    @Override
    public ScriptTypeEnum scriptType() {
        return ScriptTypeEnum.AVIATOR;
    }
}
