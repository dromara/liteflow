package com.yomahub.liteflow.springboot4.metrics;

import com.yomahub.liteflow.metrics.ChainMetricsLifeCycle;
import com.yomahub.liteflow.metrics.LiteflowMetaView;
import com.yomahub.liteflow.metrics.LiteflowMeterBinder;
import com.yomahub.liteflow.metrics.NodeMetricsLifeCycle;
import com.yomahub.liteflow.property.LiteflowConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LiteFlow 指标与结构端点装配（Spring Boot 4）。
 *
 * <p>使用 Boot4 的 {@link AutoConfiguration}；通过 afterName 排在 actuator 的
 * MetricsAutoConfiguration 与 SimpleMetricsExportAutoConfiguration 之后，
 * 确保 {@link MeterRegistry}（如 SimpleMeterRegistry）已就绪，否则本类的
 * {@code @ConditionalOnBean(MeterRegistry.class)} 装配会因评估过早而全部跳过、指标不记录。
 * 排序只写类名字符串而不引用 class 字面量，避免类路径上没有 spring-boot-actuator 时启动失败。
 *
 * @author Bryan.Zhang
 */
@AutoConfiguration(afterName = { "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration" })
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "liteflow.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
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
