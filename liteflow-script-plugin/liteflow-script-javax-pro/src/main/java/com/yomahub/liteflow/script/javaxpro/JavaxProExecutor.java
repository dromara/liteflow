package com.yomahub.liteflow.script.javaxpro;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.script.javaxpro.vo.JavaxProSettingMapKey;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import org.noear.liquor.eval.CodeSpec;
import org.noear.liquor.eval.Scripts;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Javax语言执行器，基于liquor
 * 和静态类完全一样的定义模式
 * @author Bryan.Zhang
 * @since 2.13.0
 */
public class JavaxProExecutor extends ScriptExecutor {
    private final Map<String, NodeComponent> compiledScriptMap = new CopyOnWriteHashMap<>();

    private boolean isCache;

    @Override
    public ScriptExecutor init() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        String isCacheValue = liteflowConfig.getScriptSetting().get(JavaxProSettingMapKey.IS_CACHE);
        isCache = Boolean.parseBoolean(isCacheValue);
        //如果有生命周期则执行相应生命周期实现
        super.lifeCycle(null);
        return this;
    }

    @Override
    public void load(String nodeId, String script) {
        try{
            compiledScriptMap.put(nodeId, (NodeComponent) compile(script));
        }catch (InvocationTargetException e){
            String errorMsg = StrUtil.format("script loading error for node[{}],error msg:{}", nodeId, e.getTargetException().getMessage());
            throw new ScriptLoadException(errorMsg);
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
        NodeComponent cmp = getExecutableCmp(wrap);
        cmp.process();
        return cmp.getItemResultMetaValue(wrap.slotIndex);
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
                .cached(isCache);
        return Scripts.eval(codeSpec);
    }

    @Override
    public boolean executeIsAccess(ScriptExecuteWrap wrap) {
        NodeComponent cmp = getExecutableCmp(wrap);
        return cmp.isAccess();
    }

    @Override
    public boolean executeIsContinueOnError(ScriptExecuteWrap wrap) {
        NodeComponent cmp = getExecutableCmp(wrap);
        return cmp.isContinueOnError();
    }

    @Override
    public boolean executeIsEnd(ScriptExecuteWrap wrap) {
        NodeComponent cmp = getExecutableCmp(wrap);
        return cmp.isEnd();
    }

    @Override
    public void executeBeforeProcess(ScriptExecuteWrap wrap) {
        NodeComponent cmp = getExecutableCmp(wrap);
        cmp.beforeProcess();
    }

    @Override
    public void executeAfterProcess(ScriptExecuteWrap wrap) {
        NodeComponent cmp = getExecutableCmp(wrap);
        cmp.afterProcess();
    }

    @Override
    public void executeOnSuccess(ScriptExecuteWrap wrap) throws Exception {
        NodeComponent cmp = getExecutableCmp(wrap);
        cmp.onSuccess();
    }

    @Override
    public void executeOnError(ScriptExecuteWrap wrap, Exception e) throws Exception {
        NodeComponent cmp = getExecutableCmp(wrap);
        cmp.onError(e);
    }

    @Override
    public void executeRollback(ScriptExecuteWrap wrap) throws Exception {
        NodeComponent cmp = getExecutableCmp(wrap);
        cmp.rollback();
    }

    private NodeComponent getExecutableCmp(ScriptExecuteWrap wrap){
        if (!compiledScriptMap.containsKey(wrap.getNodeId())) {
            String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
            throw new ScriptLoadException(errorMsg);
        }
        NodeComponent cmp = compiledScriptMap.get(wrap.getNodeId());
        cmp.setRefNode(wrap.getCmp().getRefNode());
        cmp.setNodeId(wrap.getNodeId());
        cmp.setType(wrap.getCmp().getType());
        cmp.setSelf(cmp);
        return cmp;
    }

    private String convertScript(String script){
        //替换掉public，private，protected等修饰词
        String script1 = script.replaceAll("public class", "class")
                .replaceAll("private class", "class")
                .replaceAll("protected class", "class");

        //分析出class的具体名称
        String className = ReUtil.getGroup1("class\\s+(\\w+)\\s+(implements|extends)", script1);

        if (StrUtil.isBlank(className)){
            throw new RuntimeException("cannot find class defined");
        }

        return script1 + "\n" +
                StrUtil.format("{} item = new {}();\n", className, className) +
                "return item;";
    }
}
