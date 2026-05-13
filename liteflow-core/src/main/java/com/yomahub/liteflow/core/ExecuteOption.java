package com.yomahub.liteflow.core;

import com.yomahub.liteflow.flow.FlowEventListener;

/**
 * {@link FlowExecutor} 的执行选项参数对象。
 *
 * <p>用于在不增加方法 overload 数量的前提下，灵活组合各种执行维度——例如同时
 * 指定 {@code requestId}、{@code conversationId} 与自定义上下文。所有字段都是
 * 可选的：未设置即沿用框架默认行为。
 *
 * <p>典型用法：
 * <pre>{@code
 *     // 仅指定 conversationId（agent 连续对话场景）
 *     flowExecutor.execute2Resp("chain1", param,
 *         ExecuteOption.of().conversationId("user-1024-task-abc"));
 *
 *     // 让框架自动生成 conversationId
 *     LiteflowResponse r = flowExecutor.execute2Resp("chain1", param,
 *         ExecuteOption.of().autoConversationId());
 *     String cid = r.getConversationId(); // 取回后续调用可复用
 *
 *     // 同时指定 rid 和 cid，并附带上下文 Class
 *     flowExecutor.execute2Resp("chain1", param,
 *         ExecuteOption.of()
 *                 .requestId(rid)
 *                 .conversationId(cid)
 *                 .contextClass(MyCtx.class));
 * }</pre>
 */
public class ExecuteOption {

    private String requestId;
    private String conversationId;
    private boolean autoConversationId;
    private Class<?>[] contextBeanClasses;
    private Object[] contextBeans;
    private FlowEventListener eventListener;

    private ExecuteOption() {}

    /** 创建一个空的执行选项，所有字段均未设置。 */
    public static ExecuteOption of() {
        return new ExecuteOption();
    }

    /**
     * 指定本次执行的 requestId。{@code null} 或空字符串等价于未设置——
     * 框架会按既有逻辑自动生成。
     */
    public ExecuteOption requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * 指定本次执行的 conversationId（业务会话标识）。
     *
     * <p>主要用于 ReAct Agent 连续对话场景：同一段对话中的所有 agent
     * 共享 workspace 目录，跨次调用传入相同 conversationId 即可恢复会话。
     *
     * <p>显式调用本方法会取消 {@link #autoConversationId()} 的语义。
     */
    public ExecuteOption conversationId(String conversationId) {
        this.conversationId = conversationId;
        this.autoConversationId = false;
        return this;
    }

    /**
     * 声明本次执行需要 conversationId 但具体值由框架生成（NanoId 格式）。
     * 生成的值可通过 {@link com.yomahub.liteflow.flow.LiteflowResponse#getConversationId()}
     * 取回，调用方据此在下一次调用中传回 {@link #conversationId(String)} 以延续会话。
     *
     * <p>调用本方法会清掉之前 {@link #conversationId(String)} 设置的具体值。
     */
    public ExecuteOption autoConversationId() {
        this.autoConversationId = true;
        this.conversationId = null;
        return this;
    }

    /** 指定上下文 bean 的 Class 数组。框架会根据 Class 创建实例。 */
    public ExecuteOption contextClass(Class<?>... contextBeanClasses) {
        this.contextBeanClasses = contextBeanClasses;
        return this;
    }

    /** 指定上下文 bean 实例数组。 */
    public ExecuteOption contextBean(Object... contextBeans) {
        this.contextBeans = contextBeans;
        return this;
    }

    /**
     * 指定本次执行期间的事件监听器。
     *
     * <p>监听器会挂载到当前 {@code Slot}，组件可在 chain 执行期间通过通用事件通道
     * 推送进度、流式文本、工具调用结果等信息。未设置时不会产生额外行为。
     */
    public ExecuteOption eventListener(FlowEventListener eventListener) {
        this.eventListener = eventListener;
        return this;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public boolean isAutoConversationId() {
        return autoConversationId;
    }

    public Class<?>[] getContextBeanClasses() {
        return contextBeanClasses;
    }

    public Object[] getContextBeans() {
        return contextBeans;
    }

    public FlowEventListener getEventListener() {
        return eventListener;
    }
}
