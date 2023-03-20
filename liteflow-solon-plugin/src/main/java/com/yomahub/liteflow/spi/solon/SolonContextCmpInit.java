package com.yomahub.liteflow.spi.solon;

import com.yomahub.liteflow.spi.ContextCmpInit;

/**
 * Solon 环境容器上下文组件初始化实现
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class SolonContextCmpInit implements ContextCmpInit {

	@Override
	public void initCmp() {
		// 已在 XPluginImpl 添加组件
	}

	@Override
	public int priority() {
		return 1;
	}

}
