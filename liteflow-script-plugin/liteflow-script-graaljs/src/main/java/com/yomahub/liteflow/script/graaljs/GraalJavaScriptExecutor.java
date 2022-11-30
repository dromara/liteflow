package com.yomahub.liteflow.script.graaljs;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.script.ScriptBeanManager;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * GraalVM JavaScript脚本语言的执行器实现
 * @author zendwang
 * @since 2.9.4
 */
public class GraalJavaScriptExecutor implements ScriptExecutor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Map<String, Source> scriptMap = new CopyOnWriteHashMap<>();

    private Engine engine;

    @Override
    public ScriptExecutor init() {
        engine = Engine.create();
        return this;
    }

    @Override
    public void load(String nodeId, String script) {
        try{
            String wrapScript = StrUtil.format("function process(){{}} process();",script);
            scriptMap.put(nodeId, Source.create("js", wrapScript));
        }catch (Exception e){
            String errorMsg = StrUtil.format("script loading error for node[{}], error msg:{}", nodeId, e.getMessage());
            throw new ScriptLoadException(errorMsg);
        }
    }

    @Override
    public Object execute(ScriptExecuteWrap wrap) throws Exception{
        if (!scriptMap.containsKey(wrap.getNodeId())){
            String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
            throw new ScriptLoadException(errorMsg);
        }
        try (Context context = Context.newBuilder().allowAllAccess(true).engine(this.engine).build()) {
            Value bindings =  context.getBindings("js");
            //往脚本语言绑定表里循环增加绑定上下文的key
            //key的规则为自定义上下文的simpleName
            //比如你的自定义上下文为AbcContext，那么key就为:abcContext
            //这里不统一放一个map的原因是考虑到有些用户会调用上下文里的方法，而不是参数，所以脚本语言的绑定表里也是放多个上下文
            DataBus.getContextBeanList(wrap.getSlotIndex()).forEach(o -> {
                String key = StrUtil.lowerFirst(o.getClass().getSimpleName());
                bindings.putMember(key, o);
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
            bindings.putMember("_meta", metaMap);

            //放入用户自己定义的bean
            ScriptBeanManager.getScriptBeanMap().forEach((key, value) -> {
                if (!bindings.hasMember(key)) {
                    bindings.putMember(key, value);
                }
            });


            Value value = context.eval( scriptMap.get(wrap.getNodeId()));
            if (value.isBoolean()) {
                return value.asBoolean();
            } else if (value.isNumber()) {
                return value.asInt();
            } else if (value.isString()) {
                return value.asString();
            }
            return value;
        } catch (Exception e){
            throw e;
        }
    }

    @Override
    public void cleanCache() {
        scriptMap.clear();
    }
}
