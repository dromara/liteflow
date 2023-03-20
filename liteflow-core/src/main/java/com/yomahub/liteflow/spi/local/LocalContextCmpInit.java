package com.yomahub.liteflow.spi.local;

import com.yomahub.liteflow.spi.ContextCmpInit;

/**
 * 非Spring环境容器上下文组件初始化实现 其实非Spring没有环境容器，所以这是个空实现
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class LocalContextCmpInit implements ContextCmpInit {

	@Override
	public void initCmp() {
		// 非spring环境不用实现
	}

	@Override
	public int priority() {
		return 2;
	}

}
