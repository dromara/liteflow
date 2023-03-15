package com.yomahub.liteflow.script;

import com.yomahub.liteflow.enums.ScriptTypeEnum;

/**
 * 脚本执行器接口
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public interface ScriptExecutor {

    ScriptExecutor init();

    void load(String nodeId, String script);

    Object execute(ScriptExecuteWrap wrap) throws Exception;

    void cleanCache();

    ScriptTypeEnum scriptType();
}
