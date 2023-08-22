package com.yomahub.liteflow.script.java;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.IScriptEvaluator;
import java.util.Map;

public class JavaExecutor extends ScriptExecutor {

    private final Map<String, IScriptEvaluator> compiledScriptMap = new CopyOnWriteHashMap<>();

    @Override
    public void load(String nodeId, String script) {
        try{
            IScriptEvaluator se = CompilerFactoryFactory.getDefaultCompilerFactory(this.getClass().getClassLoader()).newScriptEvaluator();
            se.setReturnType(Object.class);
            se.setParameters(new String[] {"_meta"}, new Class[] {ScriptExecuteWrap.class});
            se.cook(script);
            compiledScriptMap.put(nodeId, se);
        }catch (Exception e){
            String errorMsg = StrUtil.format("script loading error for node[{}],error msg:{}", nodeId, e.getMessage());
            throw new ScriptLoadException(errorMsg);
        }

    }

    @Override
    public Object executeScript(ScriptExecuteWrap wrap) throws Exception {
        if (!compiledScriptMap.containsKey(wrap.getNodeId())) {
            String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
            throw new ScriptLoadException(errorMsg);
        }
        IScriptEvaluator se = compiledScriptMap.get(wrap.getNodeId());
        return se.evaluate(wrap);
    }

    @Override
    public void cleanCache() {
        compiledScriptMap.clear();
    }

    @Override
    public ScriptTypeEnum scriptType() {
        return ScriptTypeEnum.JAVA;
    }
}
