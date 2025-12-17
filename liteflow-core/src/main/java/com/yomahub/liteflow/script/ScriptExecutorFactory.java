package com.yomahub.liteflow.script;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.exception.ScriptSpiException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * 脚本执行器工厂类
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class ScriptExecutorFactory {

	private static ScriptExecutorFactory scriptExecutorFactory;

	private final Map<String, ScriptExecutor> scriptExecutorMap = new HashMap<>();

	private final String NONE_LANGUAGE = "none";

	public static ScriptExecutorFactory loadInstance() {
		if (ObjectUtil.isNull(scriptExecutorFactory)) {
			scriptExecutorFactory = new ScriptExecutorFactory();
		}
		return scriptExecutorFactory;
	}

	public ScriptExecutor getScriptExecutor(String language) {
		if (StrUtil.isBlank(language)) {
			language = NONE_LANGUAGE;
		}

		if (!scriptExecutorMap.containsKey(language)) {
			ServiceLoader<ScriptExecutor> loader = ServiceLoader.load(ScriptExecutor.class);

			ScriptExecutor scriptExecutor;
			Iterator<ScriptExecutor> it = loader.iterator();
			while (it.hasNext()) {
				scriptExecutor = it.next().init();
				if (language.equals(NONE_LANGUAGE)) {
					scriptExecutorMap.put(language, scriptExecutor);
					break;
				}
				else {
					ScriptTypeEnum scriptType = ScriptTypeEnum.getEnumByDisplayName(language);
					if (ObjectUtil.isNull(scriptType)) {
						throw new ScriptSpiException("script language config error");
					}
					if (scriptType.equals(scriptExecutor.scriptType())) {
						scriptExecutorMap.put(language, scriptExecutor);
						break;
					}
				}
			}
		}

		if (scriptExecutorMap.containsKey(language)) {
			return scriptExecutorMap.get(language);
		}
		else {
			throw new ScriptSpiException("script spi component failed to load");
		}
	}

	public void cleanScriptCache() {
		this.scriptExecutorMap.forEach((key, value) -> value.cleanCache());
	}

	/**
	 * 根据脚本类型获取已加载的脚本执行器（不会触发新加载）
	 *
	 * @param scriptType 脚本类型枚举
	 * @return 脚本执行器，如果未加载则返回null
	 */
	public ScriptExecutor getLoadedScriptExecutor(ScriptTypeEnum scriptType) {
		if (ObjectUtil.isNull(scriptType)) {
			return null;
		}
		return scriptExecutorMap.get(scriptType.getDisplayName());
	}

	/**
	 * 重新初始化指定类型的脚本执行器
	 * 此方法适用于需要在运行时更换脚本引擎配置的场景（如插件化场景下更换ClassLoader）
	 *
	 * @param language 脚本语言类型
	 * @return 重新初始化后的脚本执行器
	 */
	public ScriptExecutor reinitScriptExecutor(String language) {
		if (StrUtil.isBlank(language)) {
			language = NONE_LANGUAGE;
		}

		ScriptExecutor executor = scriptExecutorMap.get(language);
		if (ObjectUtil.isNotNull(executor)) {
			// 重新初始化
			executor.init();
			return executor;
		}
		// 如果还未加载，则走正常加载流程
		return getScriptExecutor(language);
	}

}
