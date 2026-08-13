package com.yomahub.liteflow.agent.a2a.server;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.a2a.server.executor.runner.AgentRequestOptions;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Opens one independently owned Agent runtime for each active A2A task. */
public interface A2aServerAgentFactory {

    String TRACE_ID = "liteflow.traceId";

    String TENANT_ID = "liteflow.tenantId";

    static A2aServerAgentFactory forReActAgent(
            String agentName,
            String agentDescription,
            Function<AgentRequestOptions, ReActAgent> agentProvider) {
        Objects.requireNonNull(agentName, "agentName");
        Objects.requireNonNull(agentDescription, "agentDescription");
        Objects.requireNonNull(agentProvider, "agentProvider");
        return new ReActAgentFactory(agentName, agentDescription, agentProvider);
    }

    String agentName();

    String agentDescription();

    OwnedAgentRuntime open(AgentRequestOptions options);

    interface OwnedAgentRuntime extends AutoCloseable {

        Flux<AgentEvent> stream(List<Msg> messages);

        void interrupt();

        @Override
        void close();
    }

    final class ReActAgentFactory implements A2aServerAgentFactory {

        private final String agentName;

        private final String agentDescription;

        private final Function<AgentRequestOptions, ReActAgent> agentProvider;

        private ReActAgentFactory(
                String agentName,
                String agentDescription,
                Function<AgentRequestOptions, ReActAgent> agentProvider) {
            this.agentName = agentName;
            this.agentDescription = agentDescription;
            this.agentProvider = agentProvider;
        }

        @Override
        public String agentName() {
            return agentName;
        }

        @Override
        public String agentDescription() {
            return agentDescription;
        }

        @Override
        public OwnedAgentRuntime open(AgentRequestOptions options) {
            Objects.requireNonNull(options, "options");
            ReActAgent agent = Objects.requireNonNull(
                    agentProvider.apply(options), "agentProvider returned null");
            RuntimeContext context = RuntimeContext.builder()
                    .userId(options.getUserId())
                    .sessionId(options.getSessionId())
                    .put(AgentEvent.METADATA_TASK_ID, options.getTaskId())
                    .build();
            return new OwnedAgentRuntime() {
                @Override
                public Flux<AgentEvent> stream(List<Msg> messages) {
                    copyAllowedMetadata(messages).forEach(context::put);
                    return agent.streamEvents(messages, context);
                }

                @Override
                public void interrupt() {
                    agent.interrupt(context);
                }

                @Override
                public void close() {
                    agent.close();
                }
            };
        }

        private Map<String, Object> copyAllowedMetadata(List<Msg> messages) {
            Map<String, Object> allowed = new LinkedHashMap<>();
            for (Msg message : messages) {
                copyStringMetadata(message, TRACE_ID, allowed);
                copyStringMetadata(message, TENANT_ID, allowed);
            }
            return allowed;
        }

        private void copyStringMetadata(
                Msg message, String key, Map<String, Object> destination) {
            Object value = message.getMetadata().get(key);
            if (value instanceof String) {
                destination.put(key, value);
            }
        }
    }
}
