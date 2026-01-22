package com.yomahub.liteflow.script.validator;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.common.entity.ValidationResp;
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
     * 验证脚本逻辑的公共部分
     *
     * @param script     脚本
     * @param scriptType 脚本类型
     * @return boolean
     */
    private static ValidationResp validateScript(String script, ScriptTypeEnum scriptType){
        // 未加载任何脚本模块
        if(scriptExecutors.isEmpty()){
            String errorMsg = "The loaded script modules not found.";
            return ValidationResp.fail(new RuntimeException(errorMsg));
        }

        // 指定脚本语言未加载
        if (scriptType != null && !scriptExecutors.containsKey(scriptType)) {
            String errorMsg = StrUtil.format("Specified script language {} was not found.", scriptType);
            return ValidationResp.fail(new RuntimeException(errorMsg));
        }

        // 加载多个脚本语言需要指定语言验证
        if (scriptExecutors.size() > 1 && scriptType == null) {
            String errorMsg = "The loaded script modules more than 1. Please specify the script language.";
            return ValidationResp.fail(new RuntimeException(errorMsg));
        }

        ScriptExecutor scriptExecutor = (scriptType != null) ? scriptExecutors.get(scriptType) : scriptExecutors.values().iterator().next();
        return scriptExecutor.validate(script);
    }

    /**
     * 只引入一种脚本语言时，可以不指定语言验证
     *
     * @param script 脚本
     * @return boolean
     */
    public static boolean validate(String script){
        return validateScript(script, null).isSuccess();
    }

    /**
     * 只引入一种脚本语言时，可以不指定语言验证
     *
     * @param script 脚本
     * @return ValidationResp
     */
    public static ValidationResp validateWithEx(String script){
        return validateScript(script, null);
    }

    /**
     * 指定脚本语言验证
     *
     * @param script     脚本
     * @param scriptType 脚本类型
     * @return boolean
     */
    public static boolean validate(String script, ScriptTypeEnum scriptType){
        return validateScript(script, scriptType).isSuccess();
    }

    /**
     * 指定脚本语言验证
     *
     * @param script     脚本
     * @param scriptType 脚本类型
     * @return boolean
     */
    public static ValidationResp validateWithEx(String script, ScriptTypeEnum scriptType){
        return validateScript(script, scriptType);
    }

    /**
     * 多语言脚本批量验证
     *
     * @param scripts 脚本
     * @return boolean
     */
    public static boolean validate(Map<ScriptTypeEnum, String> scripts){
        for(Map.Entry<ScriptTypeEnum, String> script : scripts.entrySet()){
            if(!validate(script.getValue(), script.getKey())){
                return false;
            }
        }
        return true;
    }
}
