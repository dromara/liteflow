package com.yomahub.liteflow.flow;

/**
 * LiteFlow 执行期间向调用方推送的通用事件。
 *
 * <p>事件本身不绑定具体组件类型。普通组件、agent 组件或未来的插件都可以通过
 * {@link FlowEventPublisher} 发布事件，调用方通过 {@code ExecuteOption} 注册监听器。
 */
public class FlowEvent {

	private final String type;
	private final String chainId;
	private final String nodeId;
	private final String requestId;
	private final String conversationId;
	private final String text;
	private final boolean last;
	private final Object data;
	private final long timestamp;

	private FlowEvent(Builder builder) {
		this.type = builder.type;
		this.chainId = builder.chainId;
		this.nodeId = builder.nodeId;
		this.requestId = builder.requestId;
		this.conversationId = builder.conversationId;
		this.text = builder.text;
		this.last = builder.last;
		this.data = builder.data;
		this.timestamp = builder.timestamp == 0L ? System.currentTimeMillis() : builder.timestamp;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getType() {
		return type;
	}

	public String getChainId() {
		return chainId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public String getRequestId() {
		return requestId;
	}

	public String getConversationId() {
		return conversationId;
	}

	public String getText() {
		return text;
	}

	public boolean isLast() {
		return last;
	}

	public Object getData() {
		return data;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public static class Builder {
		private String type;
		private String chainId;
		private String nodeId;
		private String requestId;
		private String conversationId;
		private String text;
		private boolean last;
		private Object data;
		private long timestamp;

		public Builder type(String type) {
			this.type = type;
			return this;
		}

		public Builder chainId(String chainId) {
			this.chainId = chainId;
			return this;
		}

		public Builder nodeId(String nodeId) {
			this.nodeId = nodeId;
			return this;
		}

		public Builder requestId(String requestId) {
			this.requestId = requestId;
			return this;
		}

		public Builder conversationId(String conversationId) {
			this.conversationId = conversationId;
			return this;
		}

		public Builder text(String text) {
			this.text = text;
			return this;
		}

		public Builder last(boolean last) {
			this.last = last;
			return this;
		}

		public Builder data(Object data) {
			this.data = data;
			return this;
		}

		public Builder timestamp(long timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public FlowEvent build() {
			return new FlowEvent(this);
		}
	}
}
