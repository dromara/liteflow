package com.yomahub.liteflow.script.validator;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.script.ScriptExecutor;

import java.util.*;

/**
 * 脚本验证类
 *
 * @author Ge_Zuao
 * @since 2.12.0
 */
public class ScriptValidator {

    private static final LFLog LOG = LFLoggerManager.getLogger(ScriptValidator.class);

    private static Map<ScriptTypeEnum, ScriptExecutor> scriptExecutors;

    static {
        List<ScriptExecutor> scriptExecutorList = new ArrayList<>();
        scriptExecutors = new HashMap<>();
        ServiceLoader.load(ScriptExecutor.class).forEach(scriptExecutorList::add);
        scriptExecutorList.stream()
                .peek(ScriptExecutor::init)
                .forEach(scriptExecutor -> scriptExecutors.put(scriptExecutor.scriptType(), scriptExecutor));
    }

    /**
     * 只引入一种脚本语言时，使用该语言验证
     *
     * @param script 脚本
     * @return boolean
     */
    public static boolean validate(String script){
        if(scriptExecutors.isEmpty()){
            LOG.error("The loaded script modules not found.");
            return false;
        }
        // 使用多脚本语言需要指定验证语言
        if(scriptExecutors.size() > 1){
            LOG.error("The loaded script modules more than 1. Please specify the script language.");
            return false;
        }

        ScriptExecutor scriptExecutor = scriptExecutors.values().iterator().next();
        try {
            scriptExecutor.compile(script);
        } catch (Exception e) {
            LOG.error("script component validate failure. " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 多语言脚本验证
     *
     * @param scripts 脚本
     * @return boolean
     */
    public static boolean validate(Map<String, ScriptTypeEnum> scripts){
        for(Map.Entry<String, ScriptTypeEnum> script : scripts.entrySet()){
            ScriptExecutor scriptExecutor = scriptExecutors.getOrDefault(script.getValue(), null);
            if(scriptExecutor == null){
                LOG.error(StrUtil.format("Specified script language {} was not found.", script.getValue()));
                return false;
            }
            try {
                scriptExecutor.compile(script.getKey());
            } catch (Exception e) {
                LOG.error(StrUtil.format("{} script component validate failure. ", script.getValue()) + e.getMessage());
                return false;
            }
        }
        return true;
    }
}
