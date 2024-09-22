package com.yomahub.liteflow.script.javax;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import org.noear.liquor.eval.CodeSpec;
import org.noear.liquor.eval.ScriptEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JavaxExecutor extends ScriptExecutor {
    private final Map<String, CodeSpec> compiledScriptMap = new CopyOnWriteHashMap<>();

    private ScriptEvaluator scriptEvaluator;

    @Override
    public ScriptExecutor init() {
        scriptEvaluator = new ScriptEvaluator();
        scriptEvaluator.setPrintable(true);
        return this;
    }

    @Override
    public void load(String nodeId, String script) {
        try{
            compiledScriptMap.put(nodeId, (CodeSpec) compile(script));
        }catch (Exception e){
            String errorMsg = StrUtil.format("script loading error for node[{}],error msg:{}", nodeId, e.getMessage());
            throw new ScriptLoadException(errorMsg);
        }

    }

    @Override
    public void unLoad(String nodeId) {
        compiledScriptMap.remove(nodeId);
    }

    @Override
    public List<String> getNodeIds() {
        return new ArrayList<>(compiledScriptMap.keySet());
    }

    @Override
    public Object executeScript(ScriptExecuteWrap wrap) throws Exception {
        if (!compiledScriptMap.containsKey(wrap.getNodeId())) {
            String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
            throw new ScriptLoadException(errorMsg);
        }
        CodeSpec codeSpec = compiledScriptMap.get(wrap.getNodeId());
        return scriptEvaluator.eval(codeSpec, wrap);
    }

    @Override
    public void cleanCache() {
        compiledScriptMap.clear();
    }

    @Override
    public ScriptTypeEnum scriptType() {
        return ScriptTypeEnum.JAVA;
    }

    @Override
    public Object compile(String script) throws Exception {
        CodeSpec codeSpec = new CodeSpec(convertScript(script))
                .returnType(Object.class)
                .parameters(new String[] {"_meta"}, new Class[] {ScriptExecuteWrap.class});
        scriptEvaluator.getClazz(codeSpec);
        return codeSpec;
    }

    private String convertScript(String script){
        //替换掉public，private，protected等修饰词
        String script1 = script.replaceAll("public class", "class")
                .replaceAll("private class", "class")
                .replaceAll("protected class", "class");

        //分析出class的具体名称
        String className = ReUtil.getGroup1("class\\s+(\\w+)\\s+implements", script1);

        if (StrUtil.isBlank(className)){
            throw new RuntimeException("cannot find class defined");
        }

        return "import com.yomahub.liteflow.script.body.CommonScriptBody;\n" +
                script1 + "\n" +
                StrUtil.format("{} item = new {}();\n", className, className) +
                "if (CommonScriptBody.class.isInstance(item)){item.body(_meta);return null;}else{return item.body(_meta);}";
    }
}
