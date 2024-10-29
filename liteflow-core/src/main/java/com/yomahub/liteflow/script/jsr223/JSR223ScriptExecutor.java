package com.yomahub.liteflow.script.jsr223;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSR223 script engine的统一实现抽象类
 *
 * @author Bryan.Zhang
 * @since 2.9.5
 */
public abstract class JSR223ScriptExecutor extends ScriptExecutor {

	protected final LFLog LOG = LFLoggerManager.getLogger(this.getClass());

	private ScriptEngine scriptEngine;

	private final Map<String, CompiledScript> compiledScriptMap = new CopyOnWriteHashMap<>();

	@Override
	public ScriptExecutor init() {
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		scriptEngine = scriptEngineManager.getEngineByName(this.scriptType().getEngineName());

		//如果有生命周期则执行相应生命周期实现
		super.lifeCycle(scriptEngine);

		return this;
	}

	protected String convertScript(String script) {
		return script;
	}

	@Override
	public void load(String nodeId, String script) {
		try {
			compiledScriptMap.put(nodeId, (CompiledScript) compile(script));
		}
		catch (Exception e) {
			String errorMsg = StrUtil.format("script loading error for node[{}], error msg:{}", nodeId, e.getMessage());
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

		CompiledScript compiledScript = compiledScriptMap.get(wrap.getNodeId());
		Bindings bindings = new SimpleBindings();

		bindParam(wrap, bindings::put, bindings::putIfAbsent);

		return compiledScript.eval(bindings);
	}

	@Override
	public void cleanCache() {
		compiledScriptMap.clear();
	}

	@Override
	public Object compile(String script) throws ScriptException {
		if(scriptEngine == null) {
			LOG.error("script engine has not init");
		}
		return ((Compilable) scriptEngine).compile(convertScript(script));
	}

}
