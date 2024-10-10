package com.yomahub.liteflow.script.graaljs;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.lifecycle.LifeCycleHolder;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GraalVM JavaScript脚本语言的执行器实现
 *
 * @author zendwang
 * @since 2.9.4
 */
public class GraalJavaScriptExecutor extends ScriptExecutor {

	private final Map<String, Source> scriptMap = new CopyOnWriteHashMap<>();

	private Engine engine;

	@Override
	public ScriptExecutor init() {
		engine = Engine.create();
		//如果有生命周期则执行相应生命周期实现
		super.lifeCycle(engine);
		return this;
	}

	@Override
	public void load(String nodeId, String script) {
		try {
			scriptMap.put(nodeId, Source.create("js", (CharSequence) compile(script)));
		}
		catch (Exception e) {
			String errorMsg = StrUtil.format("script loading error for node[{}], error msg:{}", nodeId, e.getMessage());
			throw new ScriptLoadException(errorMsg);
		}
	}

	@Override
	public void unLoad(String nodeId) {
		scriptMap.remove(nodeId);
	}

	@Override
	public List<String> getNodeIds() {
		return new ArrayList<>(scriptMap.keySet());
	}

	@Override
	public Object executeScript(ScriptExecuteWrap wrap) {
		if (!scriptMap.containsKey(wrap.getNodeId())) {
			String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
			throw new ScriptLoadException(errorMsg);
		}
		try (Context context = Context.newBuilder().allowAllAccess(true).engine(this.engine).build()) {
			Value bindings = context.getBindings("js");

			bindParam(wrap, bindings::putMember, (s, o) -> {
				if (!bindings.hasMember(s)) {
					bindings.putMember(s, o);
				}
			});

			Value value = context.eval(scriptMap.get(wrap.getNodeId()));
			if (value.isBoolean()) {
				return value.asBoolean();
			}
			else if (value.isNumber()) {
				return value.asInt();
			}
			else if (value.isString()) {
				return value.asString();
			}
			return value;
		}
		catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void cleanCache() {
		scriptMap.clear();
	}

	@Override
	public ScriptTypeEnum scriptType() {
		return ScriptTypeEnum.JS;
	}

	@Override
	public Object compile(String script) throws Exception {
		String wrapScript = StrUtil.format("function process(){{}} process();", script);
		Context context = Context.newBuilder().allowAllAccess(true).engine(engine).build();
		context.parse(Source.create("js", wrapScript));
		context.close();
		return wrapScript;
	}

}
