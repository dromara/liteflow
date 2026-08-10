package com.yomahub.liteflow.agent.runtime;

import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.util.Objects;

/** A component-provided MCP client together with its LiteFlow runtime ownership. */
public record McpClientRegistration(McpClientWrapper client, boolean owned) {

    public McpClientRegistration {
        Objects.requireNonNull(client, "client");
    }
}
