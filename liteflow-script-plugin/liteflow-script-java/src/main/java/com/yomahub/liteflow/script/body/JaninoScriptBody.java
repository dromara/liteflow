package com.yomahub.liteflow.script.body;

import com.yomahub.liteflow.script.ScriptExecuteWrap;

/**
 * 这个插件已经废弃不再更新，推荐使用liteflow-script-javax或者liteflow-script-javax-pro
 */
@Deprecated
public interface JaninoScriptBody<T> {
    T body(ScriptExecuteWrap wrap);
}
