package com.yomahub.liteflow.agent.model;

import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.model.transport.HttpRequest;
import io.agentscope.core.model.transport.HttpResponse;
import io.agentscope.core.model.transport.HttpTransport;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OwnedTransportModelTest {

    @Test
    void buildRollbackKeepsPrimaryWhenTransportThrowsTheSameInstance() {
        IllegalStateException failure = new IllegalStateException("same failure");

        assertDoesNotThrow(() -> OwnedTransportModel.closeAfterBuildFailure(
                new FailingTransport(failure), failure));
        assertEquals(0, failure.getSuppressed().length);
    }

    @Test
    void closeKeepsDelegateFailureWhenTransportThrowsTheSameInstance() {
        IllegalStateException failure = new IllegalStateException("same failure");
        OwnedTransportModel model = new OwnedTransportModel(
                new CloseFailingModel(failure), new FailingTransport(failure));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, model::close);

        assertSame(failure, thrown);
        assertEquals(0, thrown.getSuppressed().length);
    }

    private static final class CloseFailingModel implements Model, AutoCloseable {
        private final RuntimeException failure;

        private CloseFailingModel(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            throw new AssertionError("offline lifecycle test must not stream");
        }

        @Override
        public String getModelName() {
            return "close-failing";
        }

        @Override
        public void close() {
            throw failure;
        }
    }

    private static final class FailingTransport implements HttpTransport {
        private final RuntimeException failure;

        private FailingTransport(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public HttpResponse execute(HttpRequest request) {
            throw new AssertionError("offline lifecycle test must not execute requests");
        }

        @Override
        public Flux<String> stream(HttpRequest request) {
            throw new AssertionError("offline lifecycle test must not stream requests");
        }

        @Override
        public void close() {
            throw failure;
        }
    }
}
