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

			// 处理 null 值
			if (value.isNull()) {
				return null;
			}

			// 处理布尔类型
			if (value.isBoolean()) {
				return value.asBoolean();
			}

			// 处理数值类型（按精度从高到低处理，避免精度丢失）
			if (value.isNumber()) {
				// 优先尝试转换为整数类型
				if (value.fitsInInt()) {
					return value.asInt();
				} else if (value.fitsInLong()) {
					return value.asLong();
				}
				// 浮点数类型
				else if (value.fitsInFloat()) {
					return value.asFloat();
				} else if (value.fitsInDouble()) {
					return value.asDouble();
				}
				// 默认返回 double（兜底方案）
				return value.asDouble();
			}

			// 处理字符串类型
			if (value.isString()) {
				return value.asString();
			}

			// 处理时间日期类型
			if (value.isDate()) {
				return value.asDate();
			}
			if (value.isTime()) {
				return value.asTime();
			}
			if (value.isInstant()) {
				return value.asInstant();
			}
			if (value.isDuration()) {
				return value.asDuration();
			}
			if (value.isTimeZone()) {
				return value.asTimeZone();
			}

			// 处理异常类型
			if (value.isException()) {
				try {
					value.throwException();
				} catch (Exception e) {
					throw new RuntimeException("Script execution threw an exception", e);
				}
			}

			// 处理 Java 主机对象（直接返回原始 Java 对象）
			if (value.isHostObject()) {
				return value.asHostObject();
			}

			// 处理数组类型（转换为 Java List）
			if (value.hasArrayElements()) {
				long size = value.getArraySize();
				List<Object> list = new ArrayList<>((int) size);
				for (long i = 0; i < size; i++) {
					list.add(value.getArrayElement(i));
				}
				return list;
			}

			// 处理对象类型（转换为 Java Map）
			if (value.hasMembers()) {
				Map<String, Object> map = new java.util.HashMap<>();
				for (String key : value.getMemberKeys()) {
					map.put(key, value.getMember(key));
				}
				return map;
			}

			// 其他类型直接返回 Value 对象
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
