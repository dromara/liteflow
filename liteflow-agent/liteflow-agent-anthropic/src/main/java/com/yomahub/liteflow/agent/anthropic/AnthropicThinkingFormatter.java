package com.yomahub.liteflow.agent.anthropic;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.anthropic.models.messages.ThinkingConfigDisabled;
import com.anthropic.models.messages.ThinkingConfigEnabled;
import com.anthropic.models.messages.ThinkingConfigParam;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicBaseFormatter;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicChatFormatter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies LiteFlow's Anthropic thinking contract at the final request boundary. */
final class AnthropicThinkingFormatter extends AnthropicBaseFormatter {

    private final AnthropicBaseFormatter delegate;

    AnthropicThinkingFormatter(AnthropicBaseFormatter delegate) {
        this.delegate = delegate == null ? new AnthropicChatFormatter() : delegate;
    }

    @Override
    protected List<MessageParam> doFormat(List<Msg> msgs) {
        return delegate.format(msgs);
    }

    @Override
    public ChatResponse parseResponse(Object response, java.time.Instant startTime) {
        return delegate.parseResponse(response, startTime);
    }

    @Override
    public void applyOptions(
            MessageCreateParams.Builder paramsBuilder,
            GenerateOptions perCall,
            GenerateOptions defaults) {
        Selection selection = select(perCall, defaults);
        delegate.applyOptions(paramsBuilder, selection.options(), null);
        if (selection.thinking() != null) {
            validateAgainstFinalRequest(paramsBuilder, selection.thinking());
            paramsBuilder.thinking(selection.thinking());
        }
    }

    @Override
    public void applyTools(MessageCreateParams.Builder paramsBuilder, List<ToolSchema> tools) {
        delegate.applyTools(paramsBuilder, tools);
    }

    @Override
    public void applySystemMessage(MessageCreateParams.Builder paramsBuilder, List<Msg> messages) {
        delegate.applySystemMessage(paramsBuilder, messages);
    }

    static Selection select(GenerateOptions perCall, GenerateOptions defaults) {
        GenerateOptions merged = GenerateOptions.mergeOptions(perCall, defaults);
        if (merged == null) {
            return new Selection(null, null);
        }
        Object raw = perCall != null && perCall.getThinkingBudget() != null
                && !hasRawThinking(perCall)
                ? null
                : rawThinking(perCall, defaults);
        Integer typed = typedThinking(perCall, defaults, raw);
        Map<String, Object> body = new LinkedHashMap<>(merged.getAdditionalBodyParams());
        body.remove("thinking");

        if (raw != null) {
            return rawSelection(merged, body, raw);
        }
        if (typed == null) {
            return new Selection(copy(merged, null, body), null);
        }
        requireMinimumBudget(typed);
        return new Selection(copy(merged, typed, body), ThinkingConfigParam.ofEnabled(
                ThinkingConfigEnabled.builder().budgetTokens(typed.longValue()).build()));
    }

    private static Object rawThinking(GenerateOptions perCall, GenerateOptions defaults) {
        if (hasRawThinking(perCall)) {
            return perCall.getAdditionalBodyParams().get("thinking");
        }
        return hasRawThinking(defaults)
                ? defaults.getAdditionalBodyParams().get("thinking")
                : null;
    }

    private static Integer typedThinking(
            GenerateOptions perCall, GenerateOptions defaults, Object raw) {
        if (hasRawThinking(perCall)) {
            return null;
        }
        if (perCall != null && perCall.getThinkingBudget() != null) {
            return perCall.getThinkingBudget();
        }
        if (raw != null) {
            return null;
        }
        return defaults == null ? null : defaults.getThinkingBudget();
    }

    private static boolean hasRawThinking(GenerateOptions options) {
        return options != null && options.getAdditionalBodyParams().containsKey("thinking");
    }

    private static Selection rawSelection(
            GenerateOptions options, Map<String, Object> body, Object raw) {
        if (!(raw instanceof Map<?, ?> source)) {
            body.put("thinking", raw);
            return new Selection(copy(options, null, body), null);
        }
        Map<String, Object> thinking = stringKeyMap(source);
        Object type = thinking.get("type");
        if ("enabled".equals(type)) {
            int budget = exactBudget(thinking.get("budget_tokens"));
            requireMinimumBudget(budget);
            ThinkingConfigEnabled.Builder builder = ThinkingConfigEnabled.builder()
                    .budgetTokens((long) budget);
            additionalProperties(thinking, builder::putAdditionalProperty);
            return new Selection(copy(options, budget, body),
                    ThinkingConfigParam.ofEnabled(builder.build()));
        }
        if ("disabled".equals(type)) {
            ThinkingConfigDisabled.Builder builder = ThinkingConfigDisabled.builder();
            additionalProperties(thinking, builder::putAdditionalProperty);
            return new Selection(copy(options, null, body),
                    ThinkingConfigParam.ofDisabled(builder.build()));
        }
        if ("adaptive".equals(type)) {
            ThinkingConfigAdaptive.Builder builder = ThinkingConfigAdaptive.builder();
            additionalProperties(thinking, builder::putAdditionalProperty);
            return new Selection(copy(options, null, body),
                    ThinkingConfigParam.ofAdaptive(builder.build()));
        }
        body.put("thinking", raw);
        return new Selection(copy(options, null, body), null);
    }

    private static Map<String, Object> stringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    private static void additionalProperties(
            Map<String, Object> values, java.util.function.BiConsumer<String, JsonValue> put) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!entry.getKey().equals("type") && !entry.getKey().equals("budget_tokens")) {
                put.accept(entry.getKey(), JsonValue.from(entry.getValue()));
            }
        }
    }

    private static int exactBudget(Object value) {
        if (!(value instanceof Number number)) {
            throw invalidBudget();
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalidBudget();
        }
    }

    private static void validateAgainstFinalRequest(
            MessageCreateParams.Builder paramsBuilder, ThinkingConfigParam thinking) {
        if (thinking.isEnabled()) {
            requireEnabledBudget(
                    thinking.asEnabled().budgetTokens(),
                    paramsBuilder.build().maxTokens());
        }
    }

    private static void requireEnabledBudget(long budget, long maxTokens) {
        requireMinimumBudget(budget);
        if (budget >= maxTokens) {
            throw invalidBudget();
        }
    }

    private static void requireMinimumBudget(long budget) {
        if (budget < 1024) {
            throw invalidBudget();
        }
    }

    private static GenerateOptions copy(
            GenerateOptions options, Integer thinkingBudget, Map<String, Object> body) {
        return GenerateOptions.builder()
                .apiKey(options.getApiKey())
                .baseUrl(options.getBaseUrl())
                .endpointPath(options.getEndpointPath())
                .modelName(options.getModelName())
                .stream(options.getStream())
                .temperature(options.getTemperature())
                .topP(options.getTopP())
                .maxTokens(options.getMaxTokens())
                .maxCompletionTokens(options.getMaxCompletionTokens())
                .frequencyPenalty(options.getFrequencyPenalty())
                .presencePenalty(options.getPresencePenalty())
                .thinkingBudget(thinkingBudget)
                .reasoningEffort(options.getReasoningEffort())
                .executionConfig(options.getExecutionConfig())
                .toolChoice(options.getToolChoice())
                .topK(options.getTopK())
                .seed(options.getSeed())
                .cacheControl(options.getCacheControl())
                .parallelToolCalls(options.getParallelToolCalls())
                .responseFormat(options.getResponseFormat())
                .additionalHeaders(options.getAdditionalHeaders())
                .additionalBodyParams(body)
                .additionalQueryParams(options.getAdditionalQueryParams())
                .build();
    }

    private static AgentConfigException invalidBudget() {
        return new AgentConfigException(
                "Anthropic enabled thinking budget must be at least 1024 and less than maxTokens");
    }

    record Selection(GenerateOptions options, ThinkingConfigParam thinking) {
    }
}
