package com.yomahub.liteflow.script.javaxpro;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.common.entity.ValidationResp;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.core.FlowInitHook;
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
import org.noear.liquor.eval.Execable;
import org.noear.liquor.eval.Scripts;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Javax语言执行器，基于liquor
 * 和静态类完全一样的定义模式
 * @author Bryan.Zhang
 * @since 2.13.0
 */
public class JavaxProExecutor extends ScriptExecutor {

    private final Map<CodeSpec, Set<String>> codeSpecMap = new HashMap<>();

    private final Map<String, NodeComponent> compiledScriptMap = new CopyOnWriteHashMap<>();

    private boolean isCache;

    @Override
    public ScriptExecutor init() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        String isCacheValue = liteflowConfig.getScriptSetting().get(JavaxProSettingMapKey.IS_CACHE);
        isCache = Boolean.parseBoolean(isCacheValue);
        //如果有生命周期则执行相应生命周期实现
        super.lifeCycle(null);

        // 注册第二段批次编译
        FlowInitHook.addHook(() -> {
            loadSecondPhase();
            codeSpecMap.clear();
            return true;
        });
        return this;
    }

    @Override
    public void load(String nodeId, String script) {
        try{
            boolean startUpPhase = FlowExecutorHolder.loadInstance().getStartUpPhase().get();
            if (startUpPhase){
                // 启动阶段：同一段脚本可能对应多个 nodeId
                CodeSpec codeSpec = (CodeSpec) compile(script);
                codeSpecMap.computeIfAbsent(codeSpec, k -> new HashSet<>())
                        .add(nodeId);
            }else{
                compiledScriptMap.put(nodeId, (NodeComponent)compile(script));
            }


        }catch (InvocationTargetException e){
            String errorMsg = StrUtil.format("script loading error for node[{}],error msg:{}", nodeId, e.getTargetException().getMessage());
            throw new ScriptLoadException(errorMsg);
        }catch (Exception e){
            String errorMsg = StrUtil.format("script loading error for node[{}],error msg:{}", nodeId, e.getMessage());
            throw new ScriptLoadException(errorMsg);
        }
    }

    // 第二段编译，指的是合并编译，如果实现了这个方法，说明compile阶段没有真正编译
    @Override
    public void loadSecondPhase() {
        if (MapUtil.isEmpty(codeSpecMap)) {
            return;
        }

        Map<CodeSpec, Execable> execableMap = Scripts.compile(new ArrayList<>(codeSpecMap.keySet()));

        execableMap.forEach((k, v) -> {
            Set<String> nodeIdSet = codeSpecMap.get(k);
            if (CollectionUtil.isEmpty(nodeIdSet)) {
                return;
            }
            nodeIdSet.forEach(nodeId -> {
                NodeComponent nodeComponent = (NodeComponent) v.exec();
                compiledScriptMap.put(nodeId, nodeComponent);
            });
        });
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
        return withExecutableCmp(wrap, cmp -> {
            cmp.process();
            return cmp.getItemResultMetaValue(wrap.slotIndex);
        });
    }

    @Override
    public void cleanCache() {
        compiledScriptMap.clear();
    }

    @Override
    public ScriptTypeEnum scriptType() {
        return ScriptTypeEnum.JAVA;
    }

    // 如果在启动时阶段，则不进行编译，只是暂存，然后由loadSecondPhase进行统一编译
    // 如果不是在启动阶段，则直接进行编译
    @Override
    public Object compile(String script) throws Exception {
        boolean startUpPhase = FlowExecutorHolder.loadInstance().getStartUpPhase().get();
        if (startUpPhase){
            return new CodeSpec(convertScript(script))
                    .returnType(Object.class)
                    .cached(isCache);
        }else{
            CodeSpec codeSpec = new CodeSpec(convertScript(script))
                    .returnType(Object.class)
                    .cached(isCache);
            return Scripts.eval(codeSpec);
        }
    }

    @Override
    public ValidationResp validate(String script){
        try {
            CodeSpec codeSpec = new CodeSpec(convertScript(script))
                    .returnType(Object.class)
                    .cached(isCache);
            Scripts.eval(codeSpec);
        } catch (Exception e) {
            return ValidationResp.fail(e);
        }
        return ValidationResp.success();
    }

    @Override
    public boolean executeIsAccess(ScriptExecuteWrap wrap) {
        return withExecutableCmp(wrap, NodeComponent::isAccess);
    }

    @Override
    public boolean executeIsContinueOnError(ScriptExecuteWrap wrap) {
        return withExecutableCmp(wrap, NodeComponent::isContinueOnError);
    }

    @Override
    public boolean executeIsEnd(ScriptExecuteWrap wrap) {
        return withExecutableCmp(wrap, NodeComponent::isEnd);
    }

    @Override
    public void executeBeforeProcess(ScriptExecuteWrap wrap) {
        runWithExecutableCmp(wrap, NodeComponent::beforeProcess);
    }

    @Override
    public void executeAfterProcess(ScriptExecuteWrap wrap) {
        runWithExecutableCmp(wrap, NodeComponent::afterProcess);
    }

    @Override
    public void executeOnSuccess(ScriptExecuteWrap wrap) throws Exception {
        runWithExecutableCmp(wrap, NodeComponent::onSuccess);
    }

    @Override
    public void executeOnError(ScriptExecuteWrap wrap, Exception e) throws Exception {
        runWithExecutableCmp(wrap, cmp -> cmp.onError(e));
    }

    @Override
    public void executeRollback(ScriptExecuteWrap wrap) throws Exception {
        runWithExecutableCmp(wrap, NodeComponent::rollback);
    }

    /**
     * 在编译出的脚本组件上执行一段有返回值的动作。
     * <p>
     * 编译产物是跨执行共享的单例，每次调用前都要把当前执行现场（refNode 等）注入进去，
     * 因此用完必须成对地清理，否则 refNodeStackTL 会随调用次数无界增长（内存泄漏）。
     * 这里是全类唯一的注入 + 清理点，新增入口只要走这个方法就不可能漏掉清理。
     *
     * @param wrap   脚本执行元参数
     * @param action 要在脚本组件上执行的动作
     * @param <T>    动作的返回值类型
     * @param <E>    动作抛出的异常类型（不抛受检异常时会推断为 RuntimeException）
     */
    private <T, E extends Exception> T withExecutableCmp(ScriptExecuteWrap wrap, CmpFunction<T, E> action) throws E {
        NodeComponent cmp = getExecutableCmp(wrap);
        try {
            return action.apply(cmp);
        } finally {
            cmp.removeRefNode();
        }
    }

    /**
     * 在编译出的脚本组件上执行一段无返回值的动作，语义同 {@link #withExecutableCmp(ScriptExecuteWrap, CmpFunction)}
     * <p>
     * 这里没有和上面的方法同名重载，是因为 lambda 形态的实参（如 {@code cmp -> cmp.onError(e)}）
     * 无法靠返回类型在两个函数式接口之间消歧，会导致编译期歧义。
     */
    private <E extends Exception> void runWithExecutableCmp(ScriptExecuteWrap wrap, CmpConsumer<E> action) throws E {
        NodeComponent cmp = getExecutableCmp(wrap);
        try {
            action.accept(cmp);
        } finally {
            cmp.removeRefNode();
        }
    }

    private NodeComponent getExecutableCmp(ScriptExecuteWrap wrap){
        String scriptNodeId = wrap.getScriptNodeId();
        if (!compiledScriptMap.containsKey(scriptNodeId)) {
            String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
            throw new ScriptLoadException(errorMsg);
        }
        NodeComponent cmp = compiledScriptMap.get(scriptNodeId);
        cmp.setRefNode(wrap.getCmp().getRefNode());
        cmp.setNodeId(wrap.getNodeId());
        cmp.setType(wrap.getCmp().getType());
        cmp.setSelf(cmp);
        return cmp;
    }

    @FunctionalInterface
    private interface CmpFunction<T, E extends Exception> {
        T apply(NodeComponent cmp) throws E;
    }

    @FunctionalInterface
    private interface CmpConsumer<E extends Exception> {
        void accept(NodeComponent cmp) throws E;
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
