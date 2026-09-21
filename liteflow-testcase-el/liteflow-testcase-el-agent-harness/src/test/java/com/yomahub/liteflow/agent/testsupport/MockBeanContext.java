package com.yomahub.liteflow.agent.testsupport;

import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import java.lang.reflect.Field;
import java.util.Map;
import static org.mockito.Mockito.*;

/** Installs only explicitly supplied test beans and restores the preceding global context. */
public final class MockBeanContext implements AutoCloseable {
    private final Field field;
    private final Object previous;

    public MockBeanContext(Map<String, Object> beans) throws Exception {
        field = ContextAwareHolder.class.getDeclaredField("contextAware");
        field.setAccessible(true);
        previous = field.get(null);
        ContextAware context = mock(ContextAware.class);
        when(context.getBean(anyString())).thenAnswer(call -> {
            String name = call.getArgument(0);
            if (!beans.containsKey(name)) throw new AssertionError("Unexpected bean: " + name);
            return beans.get(name);
        });
        field.set(null, context);
    }

    @Override public void close() throws Exception { field.set(null, previous); }
}
