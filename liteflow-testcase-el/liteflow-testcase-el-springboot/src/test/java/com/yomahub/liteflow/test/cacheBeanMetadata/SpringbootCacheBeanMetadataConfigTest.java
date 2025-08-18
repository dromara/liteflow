package com.yomahub.liteflow.test.cacheBeanMetadata;

import cn.hutool.core.util.ReflectUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.spring.DeclBeanDefinition;
import com.yomahub.liteflow.test.cacheBeanMetadata.demoComponents.DemoComponent;
import com.yomahub.liteflow.test.component.FlowExecutorELSpringbootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@TestPropertySource(value = "classpath:/cacheBeanMetadata/application.properties")
@SpringBootTest(classes = FlowExecutorELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.cacheBeanMetadata.demoComponents"})
@ContextConfiguration(initializers = CustomSpringApplicationInitializer.class)
public class SpringbootCacheBeanMetadataConfigTest {
    @Resource
    private FlowExecutor flowExecutor;
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private DeclBeanDefinition declBeanDefinition;

    @Test
    public void test() throws InvocationTargetException, IllegalAccessException {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        BeanDefinition demoComponent = beanFactory.getMergedBeanDefinition("demoComponent");
        Method method = ReflectUtil.getMethodByName(DeclBeanDefinition.class, "getRawClassFromBeanDefinition");
        method.setAccessible(true);
        Object clz = method.invoke(declBeanDefinition, demoComponent);
        Assertions.assertNotNull(clz);
        Assertions.assertEquals(DemoComponent.class, clz);
    }

    @Test
    public void test2() {
        flowExecutor.execute2Resp("chain1");
    }
}
