package com.yomahub.liteflow.agent.gemini;

import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.ThinkingConfig;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.ToolChoice;
import io.agentscope.core.model.ToolSchema;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Adds LiteFlow's typed Gemini thinking level at the final SDK request boundary. */
final class GeminiThinkingFormatter
        implements Formatter<Content, GenerateContentResponse, GenerateContentConfig.Builder> {

    private final Formatter<Content, GenerateContentResponse, GenerateContentConfig.Builder>
            delegate;

    GeminiThinkingFormatter(
            Formatter<Content, GenerateContentResponse, GenerateContentConfig.Builder> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    Formatter<Content, GenerateContentResponse, GenerateContentConfig.Builder> delegate() {
        return delegate;
    }

    @Override
    public List<Content> format(List<Msg> msgs) {
        return delegate.format(msgs);
    }

    @Override
    public ChatResponse parseResponse(GenerateContentResponse response, Instant startTime) {
        return delegate.parseResponse(response, startTime);
    }

    @Override
    public void applyOptions(
            GenerateContentConfig.Builder configBuilder,
            GenerateOptions options,
            GenerateOptions defaultOptions) {
        delegate.applyOptions(configBuilder, options, defaultOptions);

        String level = optionOrDefault(
                options, defaultOptions, GenerateOptions::getReasoningEffort);
        Integer budget = optionOrDefault(
                options, defaultOptions, GenerateOptions::getThinkingBudget);
        if (level == null && budget == null) {
            return;
        }

        ThinkingConfig.Builder thinking = configBuilder.build()
                .thinkingConfig()
                .map(ThinkingConfig::toBuilder)
                .orElseGet(ThinkingConfig::builder)
                .includeThoughts(true);
        if (level != null) {
            thinking.thinkingLevel(level);
        }
        if (budget != null) {
            thinking.thinkingBudget(budget);
        }
        configBuilder.thinkingConfig(thinking.build());
    }

    @Override
    public void applyTools(
            GenerateContentConfig.Builder configBuilder, List<ToolSchema> tools) {
        delegate.applyTools(configBuilder, tools);
    }

    @Override
    public void applyTools(
            GenerateContentConfig.Builder configBuilder,
            List<ToolSchema> tools,
            String baseUrl,
            String modelName) {
        delegate.applyTools(configBuilder, tools, baseUrl, modelName);
    }

    @Override
    public void applyToolChoice(
            GenerateContentConfig.Builder configBuilder, ToolChoice toolChoice) {
        delegate.applyToolChoice(configBuilder, toolChoice);
    }

    @Override
    public void applyToolChoice(
            GenerateContentConfig.Builder configBuilder,
            ToolChoice toolChoice,
            String baseUrl,
            String modelName) {
        delegate.applyToolChoice(configBuilder, toolChoice, baseUrl, modelName);
    }

    private static <T> T optionOrDefault(
            GenerateOptions options,
            GenerateOptions defaultOptions,
            Function<GenerateOptions, T> accessor) {
        T value = options == null ? null : accessor.apply(options);
        return value != null || defaultOptions == null
                ? value
                : accessor.apply(defaultOptions);
    }
}
