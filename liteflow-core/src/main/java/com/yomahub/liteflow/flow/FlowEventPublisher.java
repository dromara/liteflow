package com.yomahub.liteflow.flow;

import com.yomahub.liteflow.slot.Slot;

/**
 * 当前执行 {@link Slot} 上的事件监听器挂载与发布工具。
 */
public final class FlowEventPublisher {

	private static final String LISTENER_KEY = "_flow_event_listener";

	private FlowEventPublisher() {
	}

	public static void setListener(Slot slot, FlowEventListener listener) {
		if (slot != null && listener != null) {
			slot.setAttachment(LISTENER_KEY, listener);
		}
	}

	public static boolean hasListener(Slot slot) {
		return slot != null && slot.hasAttachment(LISTENER_KEY);
	}

	public static void removeListener(Slot slot) {
		if (slot != null) {
			slot.removeAttachment(LISTENER_KEY);
		}
	}

	public static void publish(Slot slot, FlowEvent event) {
		if (slot == null || event == null) {
			return;
		}
		FlowEventListener listener = slot.getAttachment(LISTENER_KEY);
		if (listener != null) {
			listener.onEvent(event);
		}
	}
}
