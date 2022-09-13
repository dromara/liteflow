package com.yomahub.liteflow.script;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.script.exception.ScriptSpiException;

import java.util.ServiceLoader;

/**
 * 脚本执行器工厂类
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class ScriptExecutorFactory {

    private static ScriptExecutorFactory scriptExecutorFactory;

    private ScriptExecutor scriptExecutor;

    public static ScriptExecutorFactory loadInstance(){
        if (ObjectUtil.isNull(scriptExecutorFactory)){
            scriptExecutorFactory = new ScriptExecutorFactory();
        }
        return scriptExecutorFactory;
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
