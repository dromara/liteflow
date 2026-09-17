package com.yomahub.liteflow.agent.model.catalog;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.model.Model;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Runtime-owned handles to model limits. Normal checks use the shared memory cache. */
public final class ModelContextResolver {
    private final Map<Model, Resolved> models = new IdentityHashMap<>();
    private final int fallbackWindow;
    private final java.util.function.Supplier<ModelsDevCatalog> catalogProvider;

    public ModelContextResolver(List<Model> models) {
        this(models, 512 * 1024);
    }

    public ModelContextResolver(List<Model> models, int fallbackWindow) {
        this(models, fallbackWindow, ModelsDevCatalog::shared);
    }

    ModelContextResolver(List<Model> models, int fallbackWindow, java.util.function.Supplier<ModelsDevCatalog> catalogProvider) {
        if (fallbackWindow <= 0) throw new AgentConfigException("Fallback context window must be positive");
        this.fallbackWindow = fallbackWindow;
        this.catalogProvider = catalogProvider;
        for (Model model : models) this.models.put(model, prepare(model));
    }

    private Resolved prepare(Model model) {
        ModelMetadata metadata = ModelMetadata.of(model);
        if (metadata != null && metadata.contextWindow() != null) {
            return new Resolved(metadata, new ModelLimits(metadata.contextWindow(), null, null), null, false);
        }
        ModelsDevCatalog catalog = null;
        if (metadata != null && metadata.provider() != null) {
            catalog = catalogProvider.get();
            ModelLimits limits = catalog.lookup(metadata.provider(), metadata.model()).orElse(null);
            if (limits != null) return new Resolved(metadata, limits, catalog, false);
        }
        // Native/custom Model implementations may explicitly report their context window.
        // An unknown custom endpoint cannot trust a provider SDK's model-name lookup table.
        if ((metadata == null || metadata.provider() != null) && model.getContextWindowSize() > 0) {
            return new Resolved(metadata, new ModelLimits(model.getContextWindowSize(), null, null), catalog, false);
        }
        org.slf4j.LoggerFactory.getLogger(ModelContextResolver.class).warn(
                "No context metadata for model {}; using configured fallback window {}", model.getModelName(), fallbackWindow);
        return new Resolved(metadata, new ModelLimits(fallbackWindow, null, null), catalog, true);
    }

    public ModelLimits limits(Model model) {
        return context(model).limits();
    }

    /** Capacity and fallback policy come from one immutable catalog result during a refresh. */
    public Context context(Model model) {
        Resolved resolved = models.get(model);
        if (resolved == null) throw new AgentConfigException("No prepared context limits for model " + model.getModelName());
        ModelLimits current = resolved.catalog == null ? null
                : resolved.catalog.current(resolved.metadata.provider(), resolved.metadata.model()).orElse(null);
        return new Context(current != null ? current : resolved.initial, current == null && resolved.fallback);
    }

    public Integer outputBudget(Model model) {
        Resolved resolved = models.get(model);
        return resolved == null || resolved.metadata == null ? null : resolved.metadata.outputBudget();
    }

    public boolean usesFallback(Model model) {
        return context(model).fallback();
    }

    public record Context(ModelLimits limits, boolean fallback) { }

    private record Resolved(ModelMetadata metadata, ModelLimits initial, ModelsDevCatalog catalog, boolean fallback) { }
}
