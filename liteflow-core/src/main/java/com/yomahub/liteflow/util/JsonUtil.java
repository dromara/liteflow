package com.yomahub.liteflow.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.exception.JsonProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * JSON 工具类
 *
 * @author zendwang
 */
public class JsonUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JsonUtil.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private JsonUtil() {
    }

    static {
        objectMapper.setTimeZone(TimeZone.getDefault());
    }

    public static String toJsonString(Object object) {
        if (ObjectUtil.isNull(object)) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            String errMsg = StrUtil.format("Error while writing value as string[{}],reason: {}", object.getClass().getName(), e.getMessage());
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
        } catch (IOException e) {
            String errMsg = StrUtil.format("Error while parsing text [{}],reason: {}", text, e.getMessage());
            LOG.error(e.getMessage(), e);
            throw new JsonProcessException(errMsg);
        }
    }

    public static <T> T parseObject(String json, Class<T> clazz){
        if (StrUtil.isEmpty(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            String errMsg = StrUtil.format("Error while parsing text [{}],reason: {}", json, e.getMessage());
            LOG.error(e.getMessage(), e);
            throw new JsonProcessException(errMsg);
        }
    }
}
