package com.yomahub.liteflow.script.kotlin;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.jsr223.JSR223ScriptExecutor;

/**
 * Kotlin脚本执行器
 * @author DaleLee
 */
public class KotlinScriptExecutor extends JSR223ScriptExecutor {
    @Override
    public ScriptTypeEnum scriptType() {
        return ScriptTypeEnum.KOTLIN;
    }
}
