package com.yomahub.liteflow.script.body;

import com.yomahub.liteflow.script.ScriptExecuteWrap;

public interface JaninoScriptBody<T> {
    T body(ScriptExecuteWrap wrap);
}
