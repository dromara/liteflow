package com.yomahub.liteflow.spi;

import com.yomahub.liteflow.core.NodeComponent;

/**
 * LiteflowComponent注解处理器SPI接口
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public interface LiteflowComponentSupport extends SpiPriority {

	String getCmpName(Object nodeComponent);

}
