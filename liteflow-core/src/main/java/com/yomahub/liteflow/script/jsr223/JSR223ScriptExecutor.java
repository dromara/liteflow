package com.yomahub.liteflow.script.jsr223;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.script.ScriptBeanManager;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.util.Map;

/**
 * JSR223 script engine的统一实现抽象类
 * @author Bryan.Zhang
 * @since 2.9.5
 */
public abstract class JSR223ScriptExecutor implements ScriptExecutor {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private ScriptEngine scriptEngine;

    private final Map<String, CompiledScript> compiledScriptMap = new CopyOnWriteHashMap<>();

    @Override
    public ScriptExecutor init() {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        scriptEngine = scriptEngineManager.getEngineByName(scriptEngineName());
        return this;
    }

    protected abstract String scriptEngineName();

    protected String convertScript(String script){
        return script;
    }

    @Override
    public void load(String nodeId, String script) {
        try{
            CompiledScript compiledScript = ((Compilable) scriptEngine).compile(convertScript(script));
            compiledScriptMap.put(nodeId, compiledScript);
        }catch (Exception e){
            String errorMsg = StrUtil.format("script loading error for node[{}], error msg:{}", nodeId, e.getMessage());
            throw new ScriptLoadException(errorMsg);
        }

    }

    @Override
    public Object execute(ScriptExecuteWrap wrap) throws Exception{
        try{
            if (!compiledScriptMap.containsKey(wrap.getNodeId())){
                String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
                throw new ScriptLoadException(errorMsg);
            }

            CompiledScript compiledScript = compiledScriptMap.get(wrap.getNodeId());
            Bindings bindings = new SimpleBindings();

            //往脚本语言绑定表里循环增加绑定上下文的key
            //key的规则为自定义上下文的simpleName
            //比如你的自定义上下文为AbcContext，那么key就为:abcContext
            //这里不统一放一个map的原因是考虑到有些用户会调用上下文里的方法，而不是参数，所以脚本语言的绑定表里也是放多个上下文
            DataBus.getContextBeanList(wrap.getSlotIndex()).forEach(o -> {
                String key = StrUtil.lowerFirst(o.getClass().getSimpleName());
                bindings.put(key, o);
            });

            //把wrap对象转换成元数据map
            Map<String, Object> metaMap = BeanUtil.beanToMap(wrap);

            //在元数据里放入主Chain的流程参数
            Slot slot = DataBus.getSlot(wrap.getSlotIndex());
            metaMap.put("requestData", slot.getRequestData());

            //如果有隐式流程，则放入隐式流程的流程参数
            Object subRequestData = slot.getChainReqData(wrap.getCurrChainId());
            if (ObjectUtil.isNotNull(subRequestData)){
                metaMap.put("subRequestData", subRequestData);
            }

            //往脚本上下文里放入元数据
            bindings.put("_meta", metaMap);

            //放入用户自己定义的bean
            ScriptBeanManager.getScriptBeanMap().forEach(bindings::putIfAbsent);

            return compiledScript.eval(bindings);
        }catch (Exception e){
            if (ObjectUtil.isNotNull(e.getCause()) && e.getCause() instanceof LiteFlowException){
                throw (LiteFlowException)e.getCause();
            }else{
                throw e;
            }
        }
    }

    @Override
    public void cleanCache() {
        compiledScriptMap.clear();
    }
}
