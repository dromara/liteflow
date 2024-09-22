package com.yomahub.liteflow.script.body;

import com.yomahub.liteflow.script.ScriptExecuteWrap;

public interface ScriptBody<T> {
    T body(ScriptExecuteWrap wrap);
}
