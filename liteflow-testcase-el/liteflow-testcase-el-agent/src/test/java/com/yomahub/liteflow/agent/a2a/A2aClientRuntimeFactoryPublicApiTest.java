package com.yomahub.liteflow.agent.a2a;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class A2aClientRuntimeFactoryPublicApiTest {

    @Test
    void exposesOnlyTheExternallyUsableNoArgDefaultFactory() {
        List<String> publicStaticFactories = Arrays.stream(
                        A2aClientRuntimeFactory.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .map(A2aClientRuntimeFactoryPublicApiTest::signature)
                .sorted()
                .toList();

        assertEquals(List.of("defaultFactory()"), publicStaticFactories);
    }

    private static String signature(Method method) {
        String parameters = Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return method.getName() + "(" + parameters + ")";
    }
}
