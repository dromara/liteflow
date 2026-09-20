package com.yomahub.liteflow.agent.conversation;

import com.yomahub.liteflow.property.LiteflowConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Import this configuration in Spring/Boot applications to inject the conversation service. */
@Configuration
public class AgentConversationConfiguration {
    @Bean(destroyMethod = "close")
    public AgentConversationService agentConversationService(LiteflowConfig liteflowConfig) {
        return AgentConversationService.open(liteflowConfig.getAgent());
    }
}
