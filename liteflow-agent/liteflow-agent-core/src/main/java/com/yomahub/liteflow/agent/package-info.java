/**
 * LiteFlow's JDK 17 integration layer for AgentScope Java 2.0.3.
 *
 * <p>An agent component owns one lazily built, stateless runtime. Each invocation receives a
 * fresh {@code LiteFlowAgentContext} through AgentScope's {@code RuntimeContext}; build-time
 * extensions must not capture a LiteFlow Slot or other per-call state. Persistent state is routed
 * through a non-owning, agent-namespaced {@code AgentStateStore} decorator and coordinated by
 * invocation leases.
 *
 * <p>The core module provides the Agent lifecycle, typed output and events, middleware, usage and
 * skill tracking, MCP ownership, retries and fallback, HITL continuation, and guarded workspace
 * tools. The core module also includes the Harness runtime, workspace, sandbox, and compaction
 * support. The optional A2A client integration lives in a sibling module.
 */
package com.yomahub.liteflow.agent;
