package com.yomahub.liteflow.enums;

/**
 * 隐式流程类型
 *
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public enum InnerChainTypeEnum {

	// 不是隐式chain
	NONE,
	// 在串行环境中执行
	IN_SYNC,
	// 在并行环境中执行
	IN_ASYNC

}
