package com.yomahub.liteflow.script.qlexpress;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressLoader;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.InstructionSet;
import com.yomahub.liteflow.script.ScriptBeanManager;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptExecuteException;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 阿里QLExpress脚本语言的执行器实现
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class QLExpressScriptExecutor implements ScriptExecutor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private ExpressRunner expressRunner;

    private final Map<String, InstructionSet> compiledScriptMap = new CopyOnWriteHashMap<>();

    @Override
    public ScriptExecutor init() {
        expressRunner = new ExpressRunner();
        return this;
    }

    @Override
    public void load(String nodeId, String script) {
        try{
            InstructionSet instructionSet = expressRunner.getInstructionSetFromLocalCache(script);
            compiledScriptMap.put(nodeId, instructionSet);
        }catch (Exception e){
            String errorMsg = StrUtil.format("script loading error for node[{}],error msg:{}", nodeId, e.getMessage());
            throw new ScriptLoadException(errorMsg);
        }
    }

    @Override
    public Object execute(ScriptExecuteWrap wrap) {
        List<String> errorList = new ArrayList<>();
        try{
            if (!compiledScriptMap.containsKey(wrap.getNodeId())){
                String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
                throw new ScriptLoadException(errorMsg);
            }

            InstructionSet instructionSet = compiledScriptMap.get(wrap.getNodeId());
            DefaultContext<String, Object> context = new DefaultContext<>();

            //往脚本语言绑定表里循环增加绑定上下文的key
            //key的规则为自定义上下文的simpleName
            //比如你的自定义上下文为AbcContext，那么key就为:abcContext
            //这里不统一放一个map的原因是考虑到有些用户会调用上下文里的方法，而不是参数，所以脚本语言的绑定表里也是放多个上下文
            DataBus.getContextBeanList(wrap.getSlotIndex()).forEach(o -> {
                String key = StrUtil.lowerFirst(o.getClass().getSimpleName());
                context.put(key, o);
            });

            //把wrap对象转换成元数据map
            Map<String, Object> metaMap = BeanUtil.beanToMap(wrap);

            //在元数据里放入主Chain的流程参数
            Slot slot = DataBus.getSlot(wrap.getSlotIndex());
            metaMap.put("requestData", slot.getRequestData());

            //如果有隐式流程，则放入隐式流程的流程参数
            Object subRequestData = slot.getChainReqData(wrap.getCurrChainName());
            if (ObjectUtil.isNotNull(subRequestData)){
                metaMap.put("subRequestData", subRequestData);
            }

            //往脚本上下文里放入元数据
            context.putAll(metaMap);

            //放入用户自己定义的bean
            context.putAll(ScriptBeanManager.getScriptBeanMap());

            return expressRunner.execute(instructionSet, context, errorList, true, false, null);
        }catch (Exception e){
            for (String scriptErrorMsg : errorList){
                log.error("\n{}", scriptErrorMsg);
            }
            String errorMsg = StrUtil.format("script execute error for node[{}]", wrap.getNodeId());
            throw new ScriptExecuteException(errorMsg);
        }
    }

    @Override
    public void cleanCache() {
        compiledScriptMap.clear();
        expressRunner.clearExpressCache();
        ReflectUtil.setFieldValue(expressRunner,"loader",new ExpressLoader(expressRunner));
    }
}
