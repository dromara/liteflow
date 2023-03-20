package com.yomahub.liteflow.property;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

/**
 * liteflow的配置获取器
 */
public class LiteflowConfigGetter {

	private static LiteflowConfig liteflowConfig;

	public static LiteflowConfig get() {
		if (ObjectUtil.isNull(liteflowConfig)) {
			liteflowConfig = ContextAwareHolder.loadContextAware().getBean(LiteflowConfig.class);
			// 这里liteflowConfig不可能为null
			// 如果在springboot环境，由于自动装配，所以不可能为null
			// 在spring环境，如果xml没配置，在FlowExecutor的init时候就已经报错了
			// 非spring环境下，FlowExecutorHolder.loadInstance(config)的时候，会把config放入这个类的静态属性中
			if (ObjectUtil.isNull(liteflowConfig)) {
				liteflowConfig = new LiteflowConfig();
			}
		}

		return liteflowConfig;
	}

	public static void clean() {
		liteflowConfig = null;
	}

	public static void setLiteflowConfig(LiteflowConfig liteflowConfig) {
		LiteflowConfigGetter.liteflowConfig = liteflowConfig;
	}

}
