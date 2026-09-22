package com.yomahub.liteflow.springboot.metrics;

import com.yomahub.liteflow.metrics.ChainMetricsLifeCycle;
import com.yomahub.liteflow.metrics.LiteflowMetaView;
import com.yomahub.liteflow.metrics.LiteflowMeterBinder;
import com.yomahub.liteflow.metrics.NodeMetricsLifeCycle;
import com.yomahub.liteflow.property.LiteflowConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * LiteFlow 指标与结构端点装配
 *
 * <p>这里不通过 @AutoConfigureAfter 直接引用 actuator 的 MetricsAutoConfiguration/SimpleMetricsExportAutoConfiguration：
 * 自动装配类的排序注解会被 Spring Boot 提前解析，缺少 spring-boot-actuator 时无法解析这些类，
 * 会导致启动直接失败（Could not find class [...]）。改为使用 @AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)，
 * 本自动装配会被排到其他自动装配之后（MetricsAutoConfiguration 声明的是 HIGHEST_PRECEDENCE + 10），
 * 既保证 MeterRegistry 已就绪，又不依赖 actuator 是否在类路径上。
 *
 * @author Bryan.Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "liteflow.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class LiteflowMetricsAutoConfiguration {

    @Bean
    @ConditionalOnBean({ MeterRegistry.class, LiteflowConfig.class })
    @ConditionalOnMissingBean
    public LiteflowMeterBinder liteflowMeterBinder(LiteflowConfig liteflowConfig) {
        return new LiteflowMeterBinder(liteflowConfig);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean
    public ChainMetricsLifeCycle chainMetricsLifeCycle(MeterRegistry meterRegistry) {
        return new ChainMetricsLifeCycle(meterRegistry);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean
    public NodeMetricsLifeCycle nodeMetricsLifeCycle(MeterRegistry meterRegistry) {
        return new NodeMetricsLifeCycle(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public LiteflowMetaView liteflowMetaView(ObjectProvider<MeterRegistry> meterRegistry) {
        return new LiteflowMetaView(meterRegistry.getIfAvailable());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Endpoint.class)
    static class LiteflowEndpointConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public LiteflowEndpoint liteflowEndpoint(LiteflowMetaView metaView) {
            return new LiteflowEndpoint(metaView);
        }
    }
}
