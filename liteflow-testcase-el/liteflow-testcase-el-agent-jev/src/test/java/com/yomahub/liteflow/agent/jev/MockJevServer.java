package com.yomahub.liteflow.agent.jev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

final class MockJevServer implements AutoCloseable {
    static final ObjectMapper JSON = new ObjectMapper();
    final BlockingQueue<Request> requests = new LinkedBlockingQueue<>();
    final CountDownLatch release = new CountDownLatch(1);
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private final HttpServer server;
    volatile Function<Request, String> answer = request -> response("refund", 0.95);
    volatile int status = 200;
    volatile boolean stallBody;

    MockJevServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(workers);
        server.createContext("/", exchange -> {
            try {
                Request request = new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        exchange.getRequestHeaders().getFirst("Content-Type"),
                        JSON.readTree(exchange.getRequestBody()));
                requests.add(request);
                byte[] bytes = answer.apply(request).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Location", baseUrl() + "/redirected");
                exchange.sendResponseHeaders(status, bytes.length);
                if (stallBody) {
                    exchange.getResponseBody().write(bytes, 0, 1);
                    exchange.getResponseBody().flush();
                    release.await(5, TimeUnit.SECONDS);
                    exchange.getResponseBody().write(bytes, 1, bytes.length - 1);
                } else {
                    exchange.getResponseBody().write(bytes);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/gateway/v1"; }

    static String response(String choice, double confidence) {
        return response(choice, confidence, "refund", "exchange", JevSwitchComponent.NO_MATCH);
    }

    static String response(String choice, double confidence, String... options) {
        Map<String, Double> probabilities = new LinkedHashMap<>();
        for (String option : options) {
            probabilities.put(option, option.equals(choice) ? 0.8 : 0.2 / (options.length - 1));
        }
        try {
            return JSON.writeValueAsString(Map.of("model", "jev-test-resolved", "answers", Map.of(
                    "route", Map.of("type", "choice", "choice", choice,
                            "confidence", confidence, "probabilities", probabilities))));
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    @Override
    public void close() {
        release.countDown();
        server.stop(0);
        workers.shutdownNow();
    }

    record Request(String method, String path, String authorization, String contentType, JsonNode body) { }
}
