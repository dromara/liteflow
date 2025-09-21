package com.yomahub.liteflow.annotation;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.StrUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnoUtil {

    private static Map<String, Annotation> annoCacheMap = new ConcurrentHashMap<>();

    public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
        String cacheKey = StrUtil.format("{}-{}", annotatedElement, annotationType.getSimpleName());

        if (annoCacheMap.containsKey(cacheKey)){
            return (A)annoCacheMap.get(cacheKey);
        }

        A annotation = AnnotationUtil.getAnnotationAlias(annotatedElement, annotationType);

        if (annotation == null) {
            return null;
        }

        annoCacheMap.put(cacheKey, annotation);

        return annotation;
    }
}
