package com.yomahub.liteflow.springboot.metrics;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.metrics.ChainMetricsLifeCycle;
import com.yomahub.liteflow.metrics.LiteflowMetaView;
import com.yomahub.liteflow.metrics.LiteflowMeterBinder;
import com.yomahub.liteflow.metrics.NodeMetricsLifeCycle;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.springboot.config.LiteflowMainAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LiteFlow 指标与结构端点装配
 *
 * @author Bryan.Zhang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "liteflow.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
// liteflow核心未启用（无FlowExecutor）时不装配任何指标bean，否则LiteflowMeterBinder会在SpringAware上下文建立前触发FlowBus静态块导致NPE
@ConditionalOnBean(FlowExecutor.class)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class,
        LiteflowMainAutoConfiguration.class })
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
