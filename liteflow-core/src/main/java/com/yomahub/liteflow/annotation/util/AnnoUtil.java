package com.yomahub.liteflow.annotation.util;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.yomahub.liteflow.annotation.AliasFor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AnnoUtil {

    public static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
        A annotation = AnnotationUtil.getAnnotation(annotatedElement, annotationType);
        if (ObjectUtil.isNull(annotation)){
            return null;
        }

        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Object> defaultValueMap = new HashMap<>();
        Arrays.stream(ReflectUtil.getMethods(annotationType)).forEach(method -> {
            AliasFor aliasFor = AnnotationUtil.getAnnotation(method, AliasFor.class);
            if (ObjectUtil.isNotNull(aliasFor)){
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

        return annotation;
    }

    public static <A extends Annotation> Object getDefaultValue(Class<A> annotationType, String property){
        try{
            return annotationType.getMethod(property).getDefaultValue();
        }catch (Exception e){
            return null;
        }
    }
}
