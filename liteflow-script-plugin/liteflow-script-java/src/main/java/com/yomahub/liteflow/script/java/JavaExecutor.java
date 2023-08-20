package com.yomahub.liteflow.script.java;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.IScriptEvaluator;

public class JavaExecutor extends ScriptExecutor {
    @Override
    public void load(String nodeId, String script) {
        // 创建Janino脚本Evaluator
        /*IScriptEvaluator se = CompilerFactoryFactory.getDefaultCompilerFactory().newScriptEvaluator();
        // 返回值类型指定为Object以支持不同脚本
        se.setReturnType(Object.class);
        // 指定Janino脚本里的变量名及类型，为通用起见，只设置一个Object类型的变量
        se.setParameters(new String[] { JANINO_SCRIPT_PARAMETER_NAME }, new Class[] { Object.class });
        // 编译
        se.cook(script);
        // 缓存编译过的Evaluator
        compiledScriptMap.put(nodeId, se);*/
    }

    @Override
    public Object executeScript(ScriptExecuteWrap wrap) throws Exception {
        return null;
    }

    @Override
    public void cleanCache() {

    }

    @Override
    public ScriptTypeEnum scriptType() {
        return null;
    }
}
