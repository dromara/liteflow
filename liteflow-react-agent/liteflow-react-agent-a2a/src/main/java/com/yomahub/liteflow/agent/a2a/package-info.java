/**
 * Optional Agent-to-Agent integration for AgentScope Java 2.0.2.
 *
 * <p>The client component creates an isolated upstream A2A agent for every subscription because
 * the upstream client stores mutable request state on each instance. The server adapter opens one
 * owned typed runtime per active task, converts typed events only at the legacy 2.0.2 wire
 * boundary, and closes or interrupts only the matching task.
 *
 * <p>The server factory creates protocol objects only. Applications remain responsible for the
 * Web endpoint, authentication, TLS, rate limits, transport lifecycle, and endpoint-ready signal.
 */
package com.yomahub.liteflow.agent.a2a;
