package com.yomahub.liteflow.spi.local;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.spi.LiteflowComponentSupport;

/**
 * 非spring环境LiteflowComponent注解的处理器 非spring环境不支持@LiteflowComponent注解，所以返回null
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class LocalLiteflowComponentSupport implements LiteflowComponentSupport {

	@Override
	public String getCmpName(Object nodeComponent) {
		return null;
	}

	@Override
	public int priority() {
		return 2;
	}

}
