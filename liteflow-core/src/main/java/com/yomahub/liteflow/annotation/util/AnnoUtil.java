package com.yomahub.liteflow.annotation.util;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LFAliasFor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注解工具类
 * 此工具类带缓存
 *
 * @author Bryan.Zhang
 */
public class AnnoUtil {

	private static Map<String, Annotation> annoCacheMap = new ConcurrentHashMap<>();

	public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
		String cacheKey = StrUtil.format("{}-{}", annotatedElement, annotationType.getSimpleName());

		if (annoCacheMap.containsKey(cacheKey)){
			return (A)annoCacheMap.get(cacheKey);
		}

		A annotation = AnnotationUtil.getAnnotation(annotatedElement, annotationType);
		if (ObjectUtil.isNull(annotation)) {
			return null;
		}

		Map<String, String> aliasMap = new HashMap<>();
		Map<String, Object> defaultValueMap = new HashMap<>();
		Arrays.stream(ReflectUtil.getMethods(annotationType)).forEach(method -> {
			LFAliasFor aliasFor = AnnotationUtil.getAnnotation(method, LFAliasFor.class);
			if (ObjectUtil.isNotNull(aliasFor)) {
				aliasMap.put(method.getName(), aliasFor.value());
				defaultValueMap.put(method.getName(), getDefaultValue(annotationType, method.getName()));
			}
		});

		aliasMap.forEach((key, value1) -> {
			Object value = ReflectUtil.invoke(annotation, key);
			Object defaultValue = defaultValueMap.get(key);
			if (ObjectUtil.notEqual(value, defaultValue)) {
				AnnotationUtil.setValue(annotation, value1, value);
			}
		});

		annoCacheMap.put(cacheKey, annotation);

		return annotation;
	}

	private static <A extends Annotation> Object getDefaultValue(Class<A> annotationType, String property) {
		try {
			return annotationType.getMethod(property).getDefaultValue();
		}
		catch (Exception e) {
			return null;
		}
	}
}
