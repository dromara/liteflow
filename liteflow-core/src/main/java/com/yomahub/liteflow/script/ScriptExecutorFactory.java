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

}
