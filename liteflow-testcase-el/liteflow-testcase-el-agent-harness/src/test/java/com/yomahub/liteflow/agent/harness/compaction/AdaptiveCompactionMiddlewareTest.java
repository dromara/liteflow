package com.yomahub.liteflow.agent.harness.compaction;

import com.yomahub.liteflow.agent.model.catalog.ModelLimits;
import com.yomahub.liteflow.property.agent.HarnessConfig;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveCompactionMiddlewareTest {
    @TempDir Path directory;

    @Test void explicitSpecCapacityAndOutputBudgetDriveAdaptiveCompaction() {
        RecordingModel actual = new RecordingModel("private-model", 0);
        class PrivateSpec extends com.yomahub.liteflow.agent.model.ModelSpec<PrivateSpec> {
            @Override public Model resolve(com.yomahub.liteflow.property.agent.AgentConfig config) {
                return recordMetadata(actual, "private", "http://localhost/private");
            }
        }
        Model model = new PrivateSpec().contextWindow(8000).maxTokens(500)
                .resolve(new com.yomahub.liteflow.property.agent.AgentConfig());
        List<Msg> input = history();
        try (HarnessAgent harness = harness(model)) {
            invoke(harness, middleware(harness, model, null, List.of(model), .8, 524288, .9), model, input, List.of());
            assertEquals(1, actual.summaries, "explicit Spec capacity must override the large unknown-model fallback");
            List<Msg> request = actual.requests.get(actual.requests.size() - 1);
            assertSame(input.get(input.size() - 1), request.get(request.size() - 1));
        }
    }

    @Test void fallbackDefaultsAndAllThreeConfigurationValuesAreValidated() {
        HarnessConfig config = new HarnessConfig();
        assertEquals(0.8, config.getCompactionThreshold());
        assertEquals(524288, config.getCompactionFallbackContextWindow());
        assertEquals(0.9, config.getCompactionFallbackThreshold());
        config.setCompactionFallbackContextWindow(65536);
        config.setCompactionFallbackThreshold(.85);
        config.validate();
        for (double invalid : new double[]{0, 1, Double.NaN, Double.POSITIVE_INFINITY}) {
            config.setCompactionFallbackThreshold(invalid);
            assertThrows(IllegalStateException.class, config::validate);
        }
        config.setCompactionFallbackThreshold(.9);
        config.setCompactionFallbackContextWindow(0);
        assertThrows(IllegalStateException.class, config::validate);
    }

    @Test void budgetHonorsIndependentInputAndOutputLimits() {
        var result = AdaptiveCompactionMiddleware.budget(new ModelLimits(10000, 6000, 2000), null, null);
        assertTrue(result.input() <= 6000);
        assertTrue(result.input() + result.output() <= 10000);
        assertThrows(RuntimeException.class, () -> AdaptiveCompactionMiddleware.budget(
                new ModelLimits(10000, null, 2000), GenerateOptions.builder().maxTokens(3000).build(), null));
        assertThrows(RuntimeException.class, () -> AdaptiveCompactionMiddleware.budget(
                new ModelLimits(1000, null, null), null, 1000));
        assertTrue(AdaptiveCompactionMiddleware.budget(new ModelLimits(null, 5000, 1000), null, null).input() > 0);
    }

    @Test void moreThanFiftySmallMessagesDoNotTriggerCompaction() {
        RecordingModel model = new RecordingModel("large", 100000);
        List<Msg> history = new ArrayList<>();
        for (int i = 0; i < 60; i++) history.add(message("hello"));
        try (HarnessAgent harness = harness(model)) {
            invoke(harness, middleware(harness, model, null, List.of(model), .8, 524288, .9), model, history, List.of());
            assertEquals(0, model.summaries);
            assertEquals(1, model.requests.size());
            assertEquals(60, model.requests.get(0).size());
        }
    }

    @Test void fullRequestIncludingSystemAndToolsAffectsTheEstimate() {
        List<Msg> input = List.of(message("hello"));
        long plain = RequestTokenEstimator.estimate(input, List.of(), null);
        ToolSchema tool = ToolSchema.builder().name("search").description("x".repeat(5000))
                .parameters(Map.of("type", "object")).build();
        assertTrue(RequestTokenEstimator.estimate(input, List.of(tool), null) > plain + 1000);
        assertTrue(RequestTokenEstimator.estimate(List.of(system("x".repeat(5000)), message("hello")), List.of(), null) > plain + 1000);
    }

    @Test void routedModelControlsThresholdAndCompactionPreservesLatestInput() {
        RecordingModel primary = new RecordingModel("primary", 1000000);
        RecordingModel routed = new RecordingModel("routed", 8000);
        List<Msg> history = history();
        try (HarnessAgent harness = harness(primary)) {
            invoke(harness, middleware(harness, primary, null, List.of(primary, routed), .8, 524288, .9), routed, history, List.of());
            assertEquals(0, primary.requests.size());
            assertEquals(1, routed.summaries);
            List<Msg> finalRequest = routed.requests.get(routed.requests.size() - 1);
            assertSame(history.get(history.size() - 1), finalRequest.get(finalRequest.size() - 1));
            assertTrue(finalRequest.stream().anyMatch(m -> m.getTextContent().contains("summary notes")));
        }
    }

    @Test void fallbackIsCompactedAgainstItsOwnWindowBeforeItIsCalled() {
        RecordingModel primary = new RecordingModel("primary", 1000000);
        primary.failMain = true;
        RecordingModel fallback = new RecordingModel("fallback", 8000);
        Model upstreamFallbackWrapper = new Model() {
            @Override public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                return Flux.error(new AssertionError("The opaque upstream wrapper must be replaced"));
            }
            @Override public String getModelName() { return primary.getModelName(); }
        };
        try (HarnessAgent harness = harness(primary)) {
            invoke(harness, middleware(harness, primary, fallback, List.of(primary, fallback), .8, 524288, .9),
                    upstreamFallbackWrapper, history(), List.of());
            assertEquals(1, primary.requests.size());
            assertEquals(1, fallback.summaries);
            assertEquals(2, fallback.requests.size());
        }
    }

    @Test void chineseHistoryUsesTheSameEstimateForTriggerAndPreservedTail() {
        RecordingModel model = new RecordingModel("small", 8000);
        List<Msg> messages = List.of(message("上下文".repeat(234)), message("上下文".repeat(234)),
                message("上下文".repeat(234)), message("上下文".repeat(234)));
        try (HarnessAgent harness = harness(model)) {
            invoke(harness, middleware(harness, model, null, List.of(model), .8, 524288, .9), model, messages, List.of());
            assertEquals(1, model.summaries);
            List<Msg> result = model.requests.get(model.requests.size() - 1);
            assertSame(messages.get(3), result.get(result.size() - 1));
        }
    }

    @Test void preservedToolResultKeepsItsMatchingAssistantCall() {
        RecordingModel model = new RecordingModel("small", 8000);
        List<Msg> messages = new ArrayList<>(history().subList(0, 4));
        Msg call = Msg.builder().role(MsgRole.ASSISTANT).content(
                io.agentscope.core.message.ToolUseBlock.builder().id("call-1").name("lookup").input(Map.of()).build()).build();
        Msg result = Msg.builder().role(MsgRole.TOOL).content(
                io.agentscope.core.message.ToolResultBlock.builder().id("call-1").name("lookup")
                        .output(List.of(TextBlock.builder().text("a".repeat(4000)).build())).build()).build();
        messages.add(call);
        messages.add(result);
        try (HarnessAgent harness = harness(model)) {
            invoke(harness, middleware(harness, model, null, List.of(model), .8, 524288, .9), model, messages, List.of());
            assertEquals(1, model.summaries);
            List<Msg> sent = model.requests.get(model.requests.size() - 1);
            assertSame(call, sent.get(sent.size() - 2));
            assertSame(result, sent.get(sent.size() - 1));
        }
    }

    @Test void unknownModelUsesConfiguredFallbackWindowAndRatio() {
        RecordingModel model = new RecordingModel("private", 0);
        List<Msg> history = history();
        try (HarnessAgent harness = harness(model)) {
            invoke(harness, middleware(harness, model, null, List.of(model), .2, 12000, .9), model, history, List.of());
            assertEquals(0, model.summaries, "The normal .2 threshold must not apply to fallback capacity");
        }
        model.requests.clear();
        try (HarnessAgent harness = harness(model)) {
            invoke(harness, middleware(harness, model, null, List.of(model), .9, 8000, .8), model, history, List.of());
            assertEquals(1, model.summaries);
        }
    }

    @Test void summaryFailureLeavesWorkingContextUnchanged() {
        RecordingModel model = new RecordingModel("small", 8000);
        model.failSummary = true;
        List<Msg> history = history();
        try (HarnessAgent harness = harness(model)) {
            harness.getDelegate().getAgentState().contextMutable().addAll(history);
            RuntimeException error = assertThrows(RuntimeException.class, () -> invoke(harness,
                    middleware(harness, model, null, List.of(model), .8, 524288, .9), model, history, List.of()));
            assertTrue(error.getMessage().contains("summary failed"));
            assertEquals(history, harness.getDelegate().getAgentState().contextMutable());
        }
    }

    @Test void oversizedSingleInputOrFixedPromptFailsBeforeMainModelCall() {
        RecordingModel model = new RecordingModel("small", 8000);
        try (HarnessAgent harness = harness(model)) {
            var middleware = middleware(harness, model, null, List.of(model), .8, 524288, .9);
            assertThrows(RuntimeException.class, () -> invoke(harness, middleware, model,
                    List.of(message("x".repeat(50000))), List.of()));
            assertThrows(RuntimeException.class, () -> invoke(harness, middleware, model,
                    List.of(system("x".repeat(50000)), message("hello")), List.of()));
            assertTrue(model.requests.isEmpty());
        }
    }

    HarnessAgent harness(Model model) {
        return HarnessAgent.builder().name("test").model(model).workspace(directory)
                .disableCompaction().disableSubagents().build();
    }
    AdaptiveCompactionMiddleware middleware(HarnessAgent harness, Model primary, Model fallback,
            List<Model> models, double ratio, int fallbackWindow, double fallbackRatio) {
        return new AdaptiveCompactionMiddleware(harness::getWorkspaceManager, primary, fallback,
                models, ratio, fallbackWindow, fallbackRatio);
    }
    void invoke(HarnessAgent harness, AdaptiveCompactionMiddleware middleware, Model model, List<Msg> messages, List<ToolSchema> tools) {
        middleware.onModelCall(harness.getDelegate(), RuntimeContext.empty(),
                new ModelCallInput(messages, tools, null, model),
                request -> request.model().stream(request.messages(), request.tools(), request.options())
                        .thenMany(Flux.empty())).blockLast();
    }
    static List<Msg> history() {
        List<Msg> messages = new ArrayList<>();
        for (int i = 0; i < 5; i++) messages.add(message("history-" + i + " " + "a".repeat(4000)));
        return messages;
    }
    static Msg message(String text) { return Msg.builder().role(MsgRole.USER).content(TextBlock.builder().text(text).build()).build(); }
    static Msg system(String text) { return Msg.builder().role(MsgRole.SYSTEM).content(TextBlock.builder().text(text).build()).build(); }

    static class RecordingModel implements Model {
        final String name;
        final int capacity;
        final List<List<Msg>> requests = new ArrayList<>();
        int summaries;
        boolean failMain;
        boolean failSummary;
        RecordingModel(String name, int capacity) { this.name = name; this.capacity = capacity; }
        @Override public String getModelName() { return name; }
        @Override public int getContextWindowSize() { return capacity; }
        @Override public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            requests.add(List.copyOf(messages));
            boolean summary = messages.stream().anyMatch(m -> m.getTextContent().contains("Context Extraction Assistant"));
            if (summary) summaries++;
            if (summary && failSummary) return Flux.error(new IllegalStateException("summary failed"));
            if (!summary && failMain) return Flux.error(new IllegalStateException("primary failed"));
            return Flux.just(ChatResponse.builder().content(List.of(TextBlock.builder().text(summary ? "summary notes" : "done").build())).build());
        }
    }
}
