package com.yomahub.liteflow.property;

import com.yomahub.liteflow.property.agent.AgentConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LiteflowConfigAgentFieldTest {
    @Test
    void agent_field_round_trip() {
        LiteflowConfig cfg = new LiteflowConfig();
        assertNull(cfg.getAgent(), "agent defaults to null");

        AgentConfig agent = new AgentConfig();
        agent.getOpenai().setApiKey("sk-xxx");
        cfg.setAgent(agent);

        assertSame(agent, cfg.getAgent());
        assertEquals("sk-xxx", cfg.getAgent().getOpenai().getApiKey());
    }
}
