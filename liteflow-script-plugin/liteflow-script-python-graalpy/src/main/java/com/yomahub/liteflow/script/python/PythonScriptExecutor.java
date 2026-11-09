package com.yomahub.liteflow.script.python;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Python脚本语言的执行器实现
 *
 * @author Bryan.Zhang
 * @since 2.12.0
 */
public class PythonScriptExecutor extends ScriptExecutor {

	private static final String RESULT_KEY = "result";

	private final Map<String, Source> compiledScriptMap = new CopyOnWriteHashMap<>();

	private Engine engine;

	@Override
	public ScriptExecutor init() {
		engine = Engine.create();
		super.lifeCycle(engine);
		return this;
	}

	@Override
	public void load(String nodeId, String script) {
		try {
			compiledScriptMap.put(nodeId, (Source) compile(script));
		}
		catch (Exception e) {
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
	public Object executeScript(ScriptExecuteWrap wrap) {
		Source source = compiledScriptMap.get(wrap.getNodeId());
		if (source == null) {
			String errorMsg = StrUtil.format("script for node[{}] is not loaded", wrap.getNodeId());
			throw new ScriptLoadException(errorMsg);
		}

		try (Context context = Context.newBuilder("python").allowAllAccess(true).engine(engine).build()) {
			Value bindings = context.getBindings("python");
			bindParam(wrap, bindings::putMember, (name, value) -> {
				if (!bindings.hasMember(name)) {
					bindings.putMember(name, value);
				}
			});

			context.eval(source);
			Value result = bindings.getMember(RESULT_KEY);
			if (result == null || result.isNull()) {
				return null;
			}

			switch (wrap.getCmp().getType()) {
				case BOOLEAN_SCRIPT:
					return result.asBoolean();
				case FOR_SCRIPT:
					return result.asInt();
				default:
					return result.as(Object.class);
			}
		}
	}

	@Override
	public void cleanCache() {
		compiledScriptMap.clear();
	}

	@Override
	public ScriptTypeEnum scriptType() {
		return ScriptTypeEnum.PYTHON;
	}

	@Override
	public Object compile(String script) {
		Source source = Source.create("python", convertScript(script));
		try (Context context = Context.newBuilder("python").allowAllAccess(true).engine(engine).build()) {
			context.parse(source);
		}
		return source;
	}

	private String convertScript(String script) {
		String[] lineArray = script.split("\\n");
		List<String> noBlankLineList = Arrays.stream(lineArray)
			.filter(s -> !StrUtil.isBlank(s))
			.collect(Collectors.toList());
		if (noBlankLineList.isEmpty()) {
			return StrUtil.EMPTY;
		}

		String blankStr = ReUtil.getGroup0("^[ ]*", noBlankLineList.get(0));
		StringBuilder scriptSB = new StringBuilder();
		noBlankLineList.forEach(
				s -> scriptSB.append(StrUtil.format("{}\n", s.replaceFirst(blankStr, StrUtil.EMPTY))));

		return scriptSB.toString().replaceAll("(?m)^return\\b", RESULT_KEY + "=");
	}
}
