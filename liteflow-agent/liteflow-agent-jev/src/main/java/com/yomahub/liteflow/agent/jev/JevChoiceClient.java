package com.yomahub.liteflow.agent.jev;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.property.agent.JevConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Jev Choice protocol over TypeSafe's System One or OpenRouter's Decisions API. */
final class JevChoiceClient {
    private static final String QUESTION_ID = "route";
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

    private static final class Transport {
        private static final HttpClient HTTP = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private JevChoiceClient() {
    }

    static JevChoiceResult evaluate(JevConfig config, Object state, String instructions,
                                    Map<String, String> criteria) throws InterruptedException {
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.chars().anyMatch(c -> c <= 32 || c >= 127)) {
            throw new IllegalArgumentException("liteflow.agent.jev.api-key must be configured with a valid value");
        }
        String model = config.getModel();
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("liteflow.agent.jev.model must not be blank");
        }
        Duration timeout = config.getTimeout();
        long timeoutMillis;
        try {
            timeoutMillis = timeout == null ? 0 : timeout.toMillis();
        } catch (ArithmeticException invalid) {
            throw new IllegalArgumentException("liteflow.agent.jev.timeout is too large");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("liteflow.agent.jev.timeout must be at least 1ms");
        }
        URI endpoint = endpoint(config);
        byte[] body;
        try {
            JsonNode stateNode = JSON.valueToTree(state);
            if (stateNode == null || !(stateNode.isTextual() || stateNode.isObject() || stateNode.isArray())) {
                throw new IllegalArgumentException("Jev state must be a string, object or array");
            }
            body = JSON.writeValueAsBytes(Map.of(
                    "model", model, "state", stateNode,
                    "questions", Map.of(QUESTION_ID, Map.of(
                            "type", "choice", "instructions", instructions, "criteria", criteria))));
        } catch (JsonProcessingException invalid) {
            throw new IllegalArgumentException("Jev state cannot be serialized as JSON");
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        CompletableFuture<HttpResponse<String>> pending = Transport.HTTP.sendAsync(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        HttpResponse<String> response;
        try {
            // Bound the entire body read as well as waiting for response headers.
            response = pending.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutFailure) {
            pending.cancel(true);
            throw new JevInvocationException("Jev request timed out", 0, timeoutFailure);
        } catch (InterruptedException interrupted) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw interrupted;
        } catch (ExecutionException failed) {
            throw new JevInvocationException("Jev HTTP request failed", 0, failed.getCause());
        }
        if (response.statusCode() != 200) {
            // Do not include remote response bodies: they may echo credentials or business input.
            throw new JevInvocationException("Jev HTTP status " + response.statusCode(), response.statusCode(), null);
        }
        return parse(response.body(), criteria.keySet());
    }

    static URI endpoint(JevConfig config) {
        String path = config.getProvider().endpointPath();
        String baseUrl = config.getBaseUrl();
        try {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalArgumentException();
            }
            URI base = URI.create(baseUrl);
            if (!("https".equalsIgnoreCase(base.getScheme()) || "http".equalsIgnoreCase(base.getScheme()))
                    || base.getHost() == null || base.getUserInfo() != null
                    || base.getQuery() != null || base.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            return URI.create(baseUrl.replaceAll("/+$", "") + path);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("liteflow.agent.jev.base-url must be an HTTP(S) API root without credentials, query or fragment");
        }
    }

    private static JevChoiceResult parse(String body, Set<String> expectedOptions) {
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (JsonProcessingException invalid) {
            throw new JevInvocationException("Jev response is not valid JSON");
        }
        if (root == null || !root.isObject()) {
            throw new JevInvocationException("Jev response must be an object");
        }
        JsonNode answer = root.path("answers").path(QUESTION_ID);
        String model = text(root.path("model"), "model");
        if (!"choice".equals(text(answer.path("type"), "type"))) {
            throw new JevInvocationException("Jev answer type must be choice");
        }
        String choice = text(answer.path("choice"), "choice");
        if (!expectedOptions.contains(choice)) {
            throw new JevInvocationException("Jev returned a choice outside the supplied options");
        }
        double confidence = probability(answer.path("confidence"));
        JsonNode values = answer.path("probabilities");
        if (!values.isObject() || values.size() != expectedOptions.size()) {
            throw new JevInvocationException("Jev probabilities must cover exactly the supplied options");
        }
        Map<String, Double> probabilities = new LinkedHashMap<>();
        for (String option : expectedOptions) {
            probabilities.put(option, probability(values.path(option)));
        }
        double sum = probabilities.values().stream().mapToDouble(Double::doubleValue).sum();
        double max = probabilities.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        if (Math.abs(sum - 1) > 0.001 || probabilities.get(choice) + 0.000001 < max) {
            throw new JevInvocationException("Jev probability distribution is inconsistent with the selected choice");
        }
        return new JevChoiceResult(model, choice, confidence, probabilities);
    }

    private static String text(JsonNode value, String field) {
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw new JevInvocationException("Jev response requires a nonblank " + field);
        }
        return value.textValue();
    }

    private static double probability(JsonNode value) {
        if (!value.isNumber() || !Double.isFinite(value.doubleValue())
                || value.doubleValue() < 0 || value.doubleValue() > 1) {
            throw new JevInvocationException("Jev confidence and probabilities must be numbers between 0 and 1");
        }
        return value.doubleValue();
    }
}
