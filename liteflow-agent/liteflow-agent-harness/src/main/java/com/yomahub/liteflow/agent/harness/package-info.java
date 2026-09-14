/**
 * Optional AgentScope Harness 2.0.3 integration for LiteFlow Agent components.
 *
 * <p>The module adds compaction, memory, skills, typed subagent declarations, task and plan
 * support, tool-result eviction, snapshots, and policy-bound filesystems. Guarded local access is
 * path protection inside the host JVM and is not a security sandbox. Docker and custom remote
 * backends have their own deployment, authentication, isolation, and lifecycle requirements.
 *
 * <p>Each component owns one Harness runtime, while every invocation receives a fresh runtime
 * context. Conversation workspace leases and agent-namespaced state routing preserve shared
 * workspace semantics without merging different agents' persistent state.
 */
package com.yomahub.liteflow.agent.harness;
