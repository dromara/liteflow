package com.yomahub.liteflow.script;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.script.exception.ScriptSpiException;

import java.util.ServiceLoader;

public class ScriptFactory {

    private static ScriptFactory scriptFactory;

    private ScriptExecutor scriptExecutor;

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
            }else{
                throw new ScriptSpiException("script spi component failed to load");
            }
        }
        return scriptExecutor;
    }
}
