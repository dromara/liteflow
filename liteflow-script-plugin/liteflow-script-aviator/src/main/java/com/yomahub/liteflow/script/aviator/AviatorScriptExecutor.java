package com.yomahub.liteflow.script.aviator;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.jsr223.JSR223ScriptExecutor;
import java.util.List;

public class AviatorScriptExecutor extends JSR223ScriptExecutor {

    @Override
    public ScriptTypeEnum scriptType() {
        return ScriptTypeEnum.AVIATOR;
    }
}
