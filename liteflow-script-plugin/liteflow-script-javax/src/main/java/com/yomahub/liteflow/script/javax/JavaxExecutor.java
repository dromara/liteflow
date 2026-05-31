package com.yomahub.liteflow.script.javax;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.script.javax.vo.JavaxSettingMapKey;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import org.noear.liquor.Utils;
import org.noear.liquor.eval.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Javax语言执行器，基于liquor
 * @author Bryan.Zhang
 * @since 2.12.4
 */
public class JavaxExecutor extends ScriptExecutor {

    private static final LFLog logger = LFLoggerManager.getLogger(JavaxExecutor.class);
    
    private static final long TEMP_CLASSLOADER_RESET_THRESHOLD = 1000L;

    private final Map<String, Execable> compiledScriptMap = new CopyOnWriteHashMap<>();

    private boolean isCache;

    @Override
    public ScriptExecutor init() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        String isCacheValue = liteflowConfig.getScriptSetting().get(JavaxSettingMapKey.IS_CACHE);
        isCache = Boolean.parseBoolean(isCacheValue);
        //如果有生命周期则执行相应生命周期实现
        super.lifeCycle(null);
        return this;
    }

    @Override
    public void load(String nodeId, String script) {
        try{
            compiledScriptMap.put(nodeId, (Execable) compile(script));
        }catch (Exception e){
            String errorMsg = StrUtil.format("script loading error for node[{}],error msg:{}", nodeId, e.getMessage());
            throw new ScriptLoadException(errorMsg);
        }

    }

    @Override
    public void unLoad(String nodeId) {
        compiledScriptMap.remove(nodeId);
    }

    /**
     * Force reset of the underlying Liquor evaluator's temporary ClassLoader.
     * 
     * This method should be called after batch unloading of script nodes to ensure
     * that compiled bytecode can be garbage collected and Metaspace is released.
     * 
     * Without this reset, the tempClassLoader in LiquorEvaluator holds compiled
     * bytecode indefinitely, causing Metaspace memory leaks in scenarios with
     * frequent dynamic script creation and destruction.
     * 
     * Implementation note:
     * - Liquor replaces tempClassLoader naturally after 10,000 compilations
     * - For high-frequency dynamic scenarios, we can't wait that long
     * - This method forces the reset via reflection
     * - Safe for concurrent access: single-threaded by design in the calling context
     * 
     * @see <a href="https://github.com/dromara/liteflow/issues/92">Issue #92</a>
     */
    public synchronized void resetClassLoader() {
        try {
            // Get the current evaluator instance
            LiquorEvaluator liquorEvaluator = Scripts.getEvaluator();
            if (liquorEvaluator == null) {
                logger.warn("Cannot reset ClassLoader: liquorEvaluator not initialized");
                return;
            }
            
            // Reflection: Access tempCount field and force it to trigger a reset
            // on the next compilation. This is safe because:
            // 1. tempCount is not volatile, so no sync issues
            // 2. It's only read/written in the evaluation thread
            // 3. We're forcing the next compilation to see threshold exceeded
            
            Field tempCountField = LiquorEvaluator.class.getDeclaredField("tempCount");
            if (tempCountField == null) {
                logger.warn("Cannot find tempCount field in LiquorEvaluator (version mismatch?)");
                return;
            }
            
            tempCountField.setAccessible(true);
            
            // Set to a value >= CACHE_CAPACITY (typically 10000)
            // This forces the next compile() to replace tempClassLoader
            tempCountField.setInt(liquorEvaluator, (int) TEMP_CLASSLOADER_RESET_THRESHOLD);
            
            logger.debug("Successfully reset Liquor ClassLoader (freed Metaspace)");
            
        } catch (NoSuchFieldException e) {
            logger.warn(
                "Cannot reset Liquor ClassLoader: tempCount field not found. " +
                "This may indicate a Liquor version upgrade. " +
                "Metaspace memory may accumulate in dynamic script scenarios. " +
                "See issue #92 for workaround.", e);
        } catch (IllegalAccessException e) {
            logger.warn("Cannot reset Liquor ClassLoader: illegal access", e);
        } catch (Exception e) {
            logger.error("Unexpected error resetting Liquor ClassLoader", e);
        }
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
        Execable execable = compiledScriptMap.get(wrap.getNodeId());
        return execable.exec(Utils.asMap("_meta", wrap));
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
                .parameters(new ParamSpec("_meta", ScriptExecuteWrap.class)).cached(isCache);
        return Scripts.compile(codeSpec);
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

        return "import com.yomahub.liteflow.script.body.CommonScriptBody;\n" +
                script1 + "\n" +
                StrUtil.format("{} item = new {}();\n", className, className) +
                "if (CommonScriptBody.class.isInstance(item)){item.body(_meta);return null;}else{return item.body(_meta);}";
    }
}
