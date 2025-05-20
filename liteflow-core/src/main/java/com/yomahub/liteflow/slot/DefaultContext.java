package com.yomahub.liteflow.slot;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.exception.NullParamException;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Liteflow的默认上下文Bean 这个建议自己去实现一个强类型的bean，正式业务不建议用这个
 *
 * @author Bryan.Zhang
 * @since 2.7.0
 */
public class DefaultContext {

	public final ConcurrentHashMap<String, Object> dataMap = new ConcurrentHashMap<>();

	private <T> void putDataMap(String key, T t) {
		if (ObjectUtil.isNull(t)) {
			throw new NullParamException("data can't accept null param");
		}
		dataMap.put(key, t);
	}

	public boolean hasData(String key) {
		return dataMap.containsKey(key);
	}

	public <T> T getData(String key) {
		return (T) dataMap.get(key);
	}

	public <T> void setData(String key, T t) {
		putDataMap(key, t);
	}

	public ConcurrentHashMap<String, Object> getDataMap() {
		return dataMap;
	}
}
