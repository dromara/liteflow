package com.yomahub.liteflow.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yomahub.liteflow.exception.JsonProcessException;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import java.io.IOException;
import java.util.*;

/**
 * JSON 工具类
 *
 * @author zendwang
 */
public class JsonUtil {

	private static final LFLog LOG = LFLoggerManager.getLogger(JsonUtil.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private JsonUtil() {
	}

	static {
		objectMapper.setTimeZone(TimeZone.getDefault());
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		JavaTimeModule javaTimeModule = new JavaTimeModule();
		objectMapper.registerModule(javaTimeModule);
	}

	public static String toJsonString(Object object) {
		if (ObjectUtil.isNull(object)) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			String errMsg = StrUtil.format("Error while writing value as string[{}],reason: {}",
					object.getClass().getName(), e.getMessage());
			LOG.error(e.getMessage(), e);
			throw new JsonProcessException(errMsg);
		}
	}

	public static JsonNode parseObject(String text) {
		if (StrUtil.isEmpty(text)) {
			return null;
		}
		try {
			return objectMapper.readTree(text);
		}
		catch (IOException e) {
			String errMsg = StrUtil.format("Error while parsing text [{}],reason: {}", text, e.getMessage());
			LOG.error(e.getMessage(), e);
			throw new JsonProcessException(errMsg);
		}
	}

	public static <T> T parseObject(String json, Class<T> clazz) {
		if (StrUtil.isEmpty(json)) {
			return null;
		}
		try {
			return objectMapper.readValue(json, clazz);
		}
		catch (IOException e) {
			String errMsg = StrUtil.format("Error while parsing text [{}],reason: {}", json, e.getMessage());
			LOG.error(e.getMessage(), e);
			throw new JsonProcessException(errMsg);
		}
	}

	public static <T> List<T> parseList(String json, Class<T> clazz) {
		if (StrUtil.isEmpty(json)) {
			return Collections.emptyList();
		}
		try {
			CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
			return objectMapper.readValue(json, listType);
		} catch (IOException e) {
			String errMsg = StrUtil.format("Error while parsing text [{}],reason: {}", json, e.getMessage());
			LOG.error(e.getMessage(), e);
			throw new JsonProcessException(errMsg);
		}
	}
}
