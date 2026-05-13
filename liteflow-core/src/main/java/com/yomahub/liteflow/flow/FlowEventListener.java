package com.yomahub.liteflow.flow;

/**
 * LiteFlow 执行期间事件监听器。
 */
@FunctionalInterface
public interface FlowEventListener {

	void onEvent(FlowEvent event);
}
