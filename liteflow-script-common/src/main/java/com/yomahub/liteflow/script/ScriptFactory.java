package com.yomahub.liteflow.script;

import cn.hutool.core.util.ObjectUtil;

import java.util.ServiceLoader;

public class ScriptFactory {

    private static ScriptFactory scriptFactory;

    private static ScriptExecutor scriptExecutor;

    public static ScriptFactory loadInstance(){
        if (ObjectUtil.isNull(scriptFactory)){
            scriptFactory = new ScriptFactory();
        }
        return scriptFactory;
    }

    public ScriptExecutor getScriptExecutor(){
        if (ObjectUtil.isNull(scriptExecutor)){
            ServiceLoader<ScriptExecutor> loader = ServiceLoader.load(ScriptExecutor.class);
            if (loader.iterator().hasNext()){
                scriptExecutor = loader.iterator().next().init();
                return scriptExecutor;
            }
        }
        return scriptExecutor;
    }
}
