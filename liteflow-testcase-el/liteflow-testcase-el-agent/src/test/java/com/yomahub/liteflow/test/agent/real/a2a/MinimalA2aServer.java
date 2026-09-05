package com.yomahub.liteflow.test.agent.real.a2a;

import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试内嵌的最小 A2A 协议服务端（JSON-RPC over HTTP），
 * 为 guide §13 的 A2A 客户端测试提供真实远程端点。
 */
final class MinimalA2aServer implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpServer server;
    private final int port;

    /** 每次收到的 message/send 文本。 */
    final List<String> receivedTexts = new CopyOnWriteArrayList<>();
    /** 收到的消息 metadata（验证 LiteFlow 注入的四元组元数据）。 */
    final List<Map<String, Object>> receivedMetadata = new CopyOnWriteArrayList<>();
    volatile String replyPrefix = "A2A-REPLY-OK";
    volatile boolean failNext = false;

    MinimalA2aServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/", exchange -> handle(exchange));
        server.start();
    }

    String baseUrl() {
        return "http://localhost:" + port + "/";
    }

    private void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "{\"error\":\"POST only\"}");
                return;
            }
            JsonNode request = MAPPER.readTree(exchange.getRequestBody());
            String method = request.path("method").asText("");
            JsonNode id = request.hasNonNull("id") ? request.get("id") : null;

            if ("message/send".equals(method)) {
                handleSend(exchange, request, id);
            } else {
                ObjectNode body = MAPPER.createObjectNode();
                body.put("jsonrpc", "2.0");
                if (id != null) {
                    body.set("id", id);
                }
                body.putObject("error").put("code", -32601).put("message", "method not found: " + method);
                respond(exchange, 200, MAPPER.writeValueAsString(body));
            }
        } catch (Exception e) {
            respond(exchange, 500, "{\"error\":" + e.getMessage() + "}");
        }
    }

    private void handleSend(com.sun.net.httpserver.HttpExchange exchange, JsonNode request, JsonNode id)
            throws IOException {
        // A2A 0.3.x MessageSendParams: {"message": {"parts": [...], "metadata": {...}}}
        JsonNode message = request.path("params").path("message");
        StringBuilder text = new StringBuilder();
        for (JsonNode part : message.path("parts")) {
            if ("text".equals(part.path("kind").asText())) {
                text.append(part.path("text").asText());
            }
        }
        receivedTexts.add(text.toString());
        if (message.has("metadata") && message.get("metadata").isObject()) {
            receivedMetadata.add(MAPPER.convertValue(message.get("metadata"), Map.class));
        }

        ObjectNode body = MAPPER.createObjectNode();
        body.put("jsonrpc", "2.0");
        if (id != null) {
            body.set("id", id);
        }
        if (failNext) {
            failNext = false;
            body.putObject("error").put("code", -32000).put("message", "a2a upstream failure");
            respond(exchange, 200, MAPPER.writeValueAsString(body));
            return;
        }
        ObjectNode result = body.putObject("result");
        result.put("kind", "message");
        result.put("messageId", "msg-" + System.nanoTime());
        result.put("role", "agent");
        result.putArray("parts")
                .addObject()
                .put("kind", "text")
                .put("text", replyPrefix + ":" + text);
        respond(exchange, 200, MAPPER.writeValueAsString(body));
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
