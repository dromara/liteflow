package com.yomahub.liteflow.script.qlexpress;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressLoader;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.InstructionSet;
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
    public Object execute(String nodeId, int slotIndex) {
        List<String> errorList = new ArrayList<>();
        try{
            if (!compiledScriptMap.containsKey(nodeId)){
                String errorMsg = StrUtil.format("script for node[{}] is not loaded", nodeId);
                throw new RuntimeException(errorMsg);
            }

            InstructionSet instructionSet = compiledScriptMap.get(nodeId);
            Slot slot = DataBus.getSlot(slotIndex);
            DefaultContext<String, Object> context = new DefaultContext<>();
            context.put("slot", slot);
            return expressRunner.execute(instructionSet, context, errorList, true, false, null);
        }catch (Exception e){
            for (String scriptErrorMsg : errorList){
                log.error("\n{}", scriptErrorMsg);
            }
            String errorMsg = StrUtil.format("script execute error for node[{}]", nodeId);
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
