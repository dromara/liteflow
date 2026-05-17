package com.yomahub.liteflow.test.agent.unit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.agent.hook.ReActLoggingHook;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReActLoggingHookTest {

    @Test
    void postActingResultLogUsesSessionIdOnEveryLine() {
        Logger logger = (Logger) LoggerFactory.getLogger(ReActLoggingHook.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);

        try {
            ReActLoggingHook hook = new ReActLoggingHook("session-1:agent");
            ToolResultBlock result = new ToolResultBlock(
                    "tool-call-1",
                    "read_file",
                    List.of(TextBlock.builder().text("file content").build()));

            hook.onEvent(new PostActingEvent(new TestAgent(), null, null, result)).block();

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .contains(
                            "[agent:act][session-1:agent] <<< read_file 结果:",
                            "[agent:act][session-1:agent]        file content")
                    .noneMatch(message -> message.contains("[agent:act][...]"));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
        }
    }

    private static class TestAgent implements Agent {

        @Override
        public String getAgentId() {
            return "test-agent";
        }

        @Override
        public String getName() {
            return "TestAgent";
        }

        @Override
        public void interrupt() {
        }

        @Override
        public void interrupt(Msg msg) {
        }

        @Override
        public Mono<Msg> call(List<Msg> msgs) {
            return Mono.empty();
        }

        @Override
        public Mono<Msg> call(List<Msg> msgs, Class<?> structuredModel) {
            return Mono.empty();
        }

        @Override
        public Mono<Msg> call(List<Msg> msgs, JsonNode schema) {
            return Mono.empty();
        }

        @Override
        public Flux<Event> stream(List<Msg> msgs, StreamOptions options) {
            return Flux.empty();
        }

        @Override
        public Flux<Event> stream(List<Msg> msgs, StreamOptions options, Class<?> structuredModel) {
            return Flux.empty();
        }

        @Override
        public Flux<Event> stream(List<Msg> msgs, StreamOptions options, JsonNode schema) {
            return Flux.empty();
        }

        @Override
        public Mono<Void> observe(Msg msg) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> observe(List<Msg> msgs) {
            return Mono.empty();
        }
    }
}
