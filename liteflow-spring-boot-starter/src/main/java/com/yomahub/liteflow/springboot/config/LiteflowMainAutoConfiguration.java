package com.yomahub.liteflow.springboot.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.monitor.MonitorBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.spi.spring.SpringAware;
import com.yomahub.liteflow.spring.ComponentScanner;
import com.yomahub.liteflow.spring.DeclBeanDefinition;
import com.yomahub.liteflow.springboot.LiteflowExecutorInit;
import com.yomahub.liteflow.spring.LiteflowSpiInit;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 主要的业务装配器 在这个装配器里装配了执行器，执行器初始化类，监控器
 * 这个装配前置条件是需要LiteflowConfig，LiteflowPropertyAutoConfiguration以及SpringAware
 *
 * @author Bryan.Zhang
 */
@Configuration
@AutoConfigureAfter({ LiteflowPropertyAutoConfiguration.class })
@ConditionalOnBean(LiteflowConfig.class)
@ConditionalOnProperty(prefix = "liteflow", name = "enable", havingValue = "true")
@Import(SpringAware.class)
public class LiteflowMainAutoConfiguration {

	@Bean
	public DeclBeanDefinition declBeanDefinition(){
		return new DeclBeanDefinition();
	}

	// 实例化ComponentScanner
	// 多加一个SpringAware的意义是，确保在执行这个的时候，SpringAware这个bean已经被初始化
	@Bean
	public ComponentScanner componentScanner(LiteflowConfig liteflowConfig, SpringAware springAware) {
		return new ComponentScanner(liteflowConfig);
	}

	// 实例化FlowExecutor
	// 多加一个SpringAware的意义是，确保在执行这个的时候，SpringAware这个bean已经被初始化
	@Bean
	@ConditionalOnMissingBean
	public FlowExecutor flowExecutor(LiteflowConfig liteflowConfig, SpringAware springAware) {
		FlowExecutor flowExecutor = new FlowExecutor();
		flowExecutor.setLiteflowConfig(liteflowConfig);
		return flowExecutor;
	}

	// FlowExecutor的初始化工作，和实例化分开来
	// 这里写2个几乎一样的是因为无论是在PARSE_ALL_ON_START或者PARSE_ONE_ON_FIRST_EXEC模式下，都需要初始化工作
	// 换句话说，这两个只可能被执行一个
	@Bean
	@ConditionalOnProperty(prefix = "liteflow", name = "parse-mode", havingValue = "PARSE_ALL_ON_START")
	public LiteflowExecutorInit liteflowExecutorInit1(FlowExecutor flowExecutor) {
		return new LiteflowExecutorInit(flowExecutor);
	}

	@Bean
	@ConditionalOnProperty(prefix = "liteflow", name = "parse-mode", havingValue = "PARSE_ONE_ON_FIRST_EXEC")
	public LiteflowExecutorInit liteflowExecutorInit2(FlowExecutor flowExecutor) {
		return new LiteflowExecutorInit(flowExecutor);
	}

	// 实例化MonitorBus
	// 多加一个SpringAware的意义是，确保在执行这个的时候，SpringAware这个bean已经被初始化
	@Bean("monitorBus")
	@ConditionalOnProperty(prefix = "liteflow", name = "monitor.enable-log", havingValue = "true")
	public MonitorBus monitorBus(LiteflowConfig liteflowConfig, SpringAware springAware) {
		return new MonitorBus(liteflowConfig);
	}

	// 初始化 SPI ,避免多线程场景下类加载器不同导致的加载不到 SPI 实现类
	@Bean
	public LiteflowSpiInit liteflowSpiInit() {
		return new LiteflowSpiInit();
	}
}
