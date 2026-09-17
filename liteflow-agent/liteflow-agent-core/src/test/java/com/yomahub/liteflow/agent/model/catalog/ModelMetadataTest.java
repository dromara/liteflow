package com.yomahub.liteflow.agent.model.catalog;

import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelMetadataTest {
    @Test void explicitLimitsAndNativeOutputOptionsNeedNoCatalogAccess() {
        Model model = new Spec().contextWindow(64000).maxTokens(1000)
                .generateOptions(GenerateOptions.builder().maxCompletionTokens(3000).build()).resolve(new AgentConfig());
        var resolver = new ModelContextResolver(List.of(model), 524288,
                () -> { throw new AssertionError("Explicit model capacity must not access the catalog"); });
        assertEquals(64000, resolver.limits(model).context());
        assertEquals(3000, resolver.outputBudget(model));
        assertFalse(resolver.usesFallback(model));
    }

    @Test void equalModelsKeepIndependentMetadataByIdentity() {
        Model first = new EqualModel();
        Model second = new EqualModel();
        ModelMetadata.register(first, "one", 10000, null, null);
        ModelMetadata.register(second, "two", 20000, null, null);
        assertEquals("one", ModelMetadata.of(first).provider());
        assertEquals("two", ModelMetadata.of(second).provider());
    }

    @Test void providersRespectActualEndpointAndRegionalLimits() {
        assertEquals("alibaba-cn", ModelMetadata.providerFor("dashscope", null));
        assertEquals("minimax-cn", ModelMetadata.providerFor("minimax", null));
        assertEquals("minimax", ModelMetadata.providerFor("minimax", "https://api.minimax.io/v1"));
        assertEquals("openrouter", ModelMetadata.providerFor("openai", "https://openrouter.ai/api/v1"));
        assertEquals("moonshotai", ModelMetadata.providerFor("kimi", "https://api.moonshot.ai/v1"));
        assertNull(ModelMetadata.providerFor("openai", "https://private.example/v1"));
        assertNull(ModelMetadata.providerFor("openai", "https://api.openai.com.attacker.example/v1"));
    }

    private static class Spec extends ModelSpec<Spec> {
        @Override public Model resolve(AgentConfig config) {
            return recordMetadata(new EqualModel(), "openai", null);
        }
    }
    private static class EqualModel implements Model {
        @Override public String getModelName() { return "model"; }
        @Override public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) { return Flux.empty(); }
        @Override public boolean equals(Object other) { return other instanceof EqualModel; }
        @Override public int hashCode() { return 1; }
    }
}
