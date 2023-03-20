package com.yomahub.liteflow.spi;

/**
 * 环境容器中组件初始化SPI接口 分2个，非spring环境下的实现和spring体系下的实现
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public interface ContextCmpInit extends SpiPriority {

	void initCmp();

}
