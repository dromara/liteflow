package com.yomahub.liteflow.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Minimal wire-level MCP fixture, also launched as a real StdIO child JVM. */
public final class McpTestServer implements AutoCloseable {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final CountDownLatch closed = new CountDownLatch(1);
    private volatile OutputStream events;
    final AtomicInteger calls = new AtomicInteger();
    final AtomicInteger authorizedRequests = new AtomicInteger();

    McpTestServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }

    String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private void handle(HttpExchange exchange) throws java.io.IOException {
        try {
            if (!"Bearer guide-test".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                send(exchange, 403, "{\"error\":\"forbidden\"}");
                return;
            }
            authorizedRequests.incrementAndGet();
            if (exchange.getRequestURI().getPath().equals("/sse")) {
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, 0);
                events = exchange.getResponseBody();
                emit("endpoint", "/messages");
                closed.await(30, TimeUnit.SECONDS);
                return;
            }
            if (!exchange.getRequestMethod().equals("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            JsonNode request = JSON.readTree(exchange.getRequestBody());
            if (request.path("method").asText().equals("tools/call")) calls.incrementAndGet();
            ObjectNode response = reply(request);
            if (exchange.getRequestURI().getPath().equals("/messages")) {
                if (response != null) emit("message", response.toString());
                exchange.sendResponseHeaders(202, -1);
            } else if (response == null) {
                exchange.sendResponseHeaders(202, -1);
            } else {
                send(exchange, 200, response.toString());
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            exchange.close();
        }
    }

    private synchronized void emit(String type, String data) throws java.io.IOException {
        events.write(("event: " + type + "\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
        events.flush();
    }

    private static void send(HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static ObjectNode reply(JsonNode request) {
        if (!request.has("id")) return null;
        ObjectNode response = JSON.createObjectNode().put("jsonrpc", "2.0");
        response.set("id", request.get("id"));
        ObjectNode result = response.putObject("result");
        switch (request.path("method").asText()) {
            case "initialize" -> {
                result.put("protocolVersion", request.path("params").path("protocolVersion").asText("2024-11-05"));
                result.putObject("capabilities").putObject("tools").put("listChanged", false);
                result.putObject("serverInfo").put("name", "orders").put("version", "1");
            }
            case "tools/list" -> {
                ObjectNode tool = result.putArray("tools").addObject()
                        .put("name", "lookup_order").put("description", "Look up an order");
                ObjectNode schema = tool.putObject("inputSchema").put("type", "object");
                schema.putObject("properties").putObject("orderId").put("type", "string");
                schema.putArray("required").add("orderId");
                tool.putObject("annotations").put("readOnlyHint", true);
            }
            case "tools/call" -> {
                String id = request.path("params").path("arguments").path("orderId").asText();
                boolean error = "missing".equals(id);
                result.put("isError", error);
                result.putArray("content").addObject().put("type", "text")
                        .put("text", error ? "ORDER-NOT-FOUND" : "ORDER:" + id + ":PAID");
            }
            case "ping" -> { }
            default -> {
                response.remove("result");
                response.putObject("error").put("code", -32601).put("message", "unknown method");
            }
        }
        return response;
    }

    public static void main(String[] args) throws Exception {
        try (var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            for (String line; (line = input.readLine()) != null;) {
                ObjectNode response = reply(JSON.readTree(line));
                if (response != null) {
                    System.out.println(response);
                    System.out.flush();
                }
            }
        }
    }

    @Override public void close() {
        closed.countDown();
        server.stop(0);
        executor.shutdownNow();
    }
}
