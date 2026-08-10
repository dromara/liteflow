/**
 * LiteFlow ReAct Agent 核心模块。
 *
 * <p>The AgentScope 2 runtime is component-owned and reuses one stateless Agent across calls.
 * Invocation identity and LiteFlow Slot data are propagated only through per-call
 * {@code RuntimeContext}. Persistent Agent state is isolated by a non-owning namespaced
 * StateStore decorator.
 */
package com.yomahub.liteflow.agent;
