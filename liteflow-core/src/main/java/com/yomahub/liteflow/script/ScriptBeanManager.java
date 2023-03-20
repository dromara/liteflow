package com.yomahub.liteflow.script;

import java.util.HashMap;
import java.util.Map;

/**
 * Script中可使用的java bean管理类
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class ScriptBeanManager {

	private static final Map<String, Object> scriptBeanMap = new HashMap<>();

	public static void addScriptBean(String key, Object bean) {
		scriptBeanMap.put(key, bean);
	}

	public static Map<String, Object> getScriptBeanMap() {
		return scriptBeanMap;
	}

}
