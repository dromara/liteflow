package com.yomahub.liteflow.script.body;

import com.yomahub.liteflow.script.ScriptExecuteWrap;

/**
 * Javax语言脚本继承类的接口
 * @author Bryan.Zhang
 * @since 2.12.4
 */
public interface ScriptBody<T> {
    T body(ScriptExecuteWrap wrap);
}
