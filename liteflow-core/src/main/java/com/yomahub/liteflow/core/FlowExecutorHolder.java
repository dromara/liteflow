package com.yomahub.liteflow.core;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.exception.FlowExecutorNotInitException;
import com.yomahub.liteflow.property.LiteflowConfig;

/**
 * @author Bryan.Zhang
 */
public class FlowExecutorHolder {

	private static FlowExecutor flowExecutor;

	public static FlowExecutor loadInstance(LiteflowConfig liteflowConfig) {
		if (ObjectUtil.isNull(flowExecutor)) {
			flowExecutor = new FlowExecutor(liteflowConfig);
		}
		return flowExecutor;
	}

	public static FlowExecutor loadInstance() {
		if (ObjectUtil.isNull(flowExecutor)) {
			throw new FlowExecutorNotInitException("flow executor is not initialized yet");
		}
		return flowExecutor;
	}

	public static void setHolder(FlowExecutor flowExecutor) {
		FlowExecutorHolder.flowExecutor = flowExecutor;
	}

	public static void clean() {
		flowExecutor = null;
	}

}
