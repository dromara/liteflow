package com.yomahub.liteflow.agent.model.catalog;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ModelsDevCatalogTest {
    @TempDir Path directory;
    HttpServer server;
    ModelsDevCatalog catalog;
    final MutableClock clock = new MutableClock();
    final AtomicInteger requests = new AtomicInteger();
    volatile String etag = "\"one\"";
    volatile String receivedEtag;
    volatile String body = data(10000);
    volatile boolean conditional = true;
    volatile int status = 200;

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api.json", exchange -> {
            requests.incrementAndGet();
            receivedEtag = exchange.getRequestHeaders().getFirst("If-None-Match");
            exchange.getResponseHeaders().set("ETag", etag);
            if (conditional && etag.equals(receivedEtag)) {
                exchange.sendResponseHeaders(304, -1);
            } else if (status != 200) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                    gzip.write(body.getBytes(StandardCharsets.UTF_8));
                }
                exchange.getResponseHeaders().set("Content-Encoding", "gzip");
                exchange.sendResponseHeaders(200, bytes.size());
                exchange.getResponseBody().write(bytes.toByteArray());
            }
            exchange.close();
        });
        server.start();
        catalog = create();
    }

    ModelsDevCatalog create() {
        return new ModelsDevCatalog(directory.resolve("models.json"),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api.json"),
                Duration.ofHours(6), Duration.ofMinutes(5), clock);
    }

    @AfterEach void stop() {
        catalog.close();
        server.stop(0);
    }

    @Test void downloadsOnceProjectsAllModelsAndKeepsOnlyQueriedEntriesInMemory() throws Exception {
        assertEquals(10000, catalog.lookup("p", "a").orElseThrow().context());
        String disk = Files.readString(directory.resolve("models.json"));
        assertFalse(disk.contains("pricing"));
        assertFalse(disk.contains("SECRET-UNNEEDED-DESCRIPTION"));
        assertTrue(disk.contains("other-provider"));
        assertEquals(1, catalog.cachedEntries());
        assertEquals(20000, catalog.lookup("other-provider", "a").orElseThrow().context());
        assertEquals(2, catalog.cachedEntries());
        assertEquals(1, requests.get());
        Files.delete(directory.resolve("models.json"));
        for (int i = 0; i < 1000; i++) assertEquals(10000, catalog.cached("p", "a").orElseThrow().context());
        assertEquals(10000, catalog.lookup("p", "a").orElseThrow().context());
        assertEquals(1, requests.get());
    }

    @Test void concurrentFirstLookupsShareOneDownload() {
        List<CompletableFuture<ModelLimits>> work = new ArrayList<>();
        for (int i = 0; i < 20; i++) work.add(CompletableFuture.supplyAsync(() -> catalog.lookup("p", "a").orElseThrow()));
        work.forEach(future -> assertEquals(10000, future.join().context()));
        assertEquals(1, requests.get());
    }

    @Test void restartUsesDiskAndConditionalRefreshDoesNotRewriteSnapshot() throws Exception {
        catalog.lookup("p", "a");
        String previous = Files.readString(directory.resolve("models.json"));
        catalog.close();
        catalog = create();
        assertEquals(10000, catalog.lookup("p", "a").orElseThrow().context());
        assertEquals(1, requests.get());
        clock.advance(Duration.ofHours(7));
        catalog.refresh(false);
        assertEquals("\"one\"", receivedEtag);
        assertEquals(2, requests.get());
        assertEquals(previous, Files.readString(directory.resolve("models.json")));
    }

    @Test void changedSnapshotRefreshesActiveEntriesAndMakesNewModelsAvailable() {
        catalog.lookup("p", "a");
        clock.advance(Duration.ofHours(7));
        body = data(50000);
        etag = "\"two\"";
        catalog.refresh(false);
        assertEquals(50000, catalog.cached("p", "a").orElseThrow().context());
        assertEquals(1, catalog.cachedEntries());
        assertNull(catalog.lookup("p", "input-only").orElseThrow().context());
        assertEquals(1234, catalog.lookup("p", "input-only").orElseThrow().input());
        assertEquals(2, requests.get());
    }

    @Test void malformedDownloadAndHttpFailurePreserveTheLastCompleteSnapshot() throws Exception {
        catalog.lookup("p", "a");
        String previous = Files.readString(directory.resolve("models.json"));
        conditional = false;
        for (String invalid : List.of("{", "{}", "{\"p\":{\"models\":{\"a\":{\"limit\":{\"context\":4000}}}}} trailing")) {
            clock.advance(Duration.ofHours(7));
            body = invalid;
            catalog.refresh(false);
            assertEquals(previous, Files.readString(directory.resolve("models.json")));
            assertEquals(10000, catalog.cached("p", "a").orElseThrow().context());
        }
        clock.advance(Duration.ofHours(7));
        status = 503;
        catalog.refresh(false);
        assertEquals(previous, Files.readString(directory.resolve("models.json")));
    }

    @Test void missingModelRefreshesEarlyWithGlobalCooldownAndBoundedNegativeCache() {
        catalog.lookup("p", "a");
        clock.advance(Duration.ofMinutes(6));
        assertTrue(catalog.lookup("p", "new-model").isEmpty());
        assertEquals(2, requests.get());
        for (int i = 0; i < 1000; i++) assertTrue(catalog.lookup("p", "missing-" + i).isEmpty());
        assertEquals(2, requests.get());
        assertEquals(256, catalog.cachedEntries());
        clock.advance(Duration.ofMinutes(6));
        assertTrue(catalog.lookup("p", "missing-999").isEmpty());
        assertEquals(3, requests.get());
    }

    @Test void failedFirstDownloadCanRecoverWithoutRestart() {
        status = 503;
        assertTrue(catalog.lookup("p", "a").isEmpty());
        assertEquals(1, requests.get());
        status = 200;
        clock.advance(Duration.ofMinutes(6));
        assertEquals(10000, catalog.lookup("p", "a").orElseThrow().context());
        assertEquals(2, requests.get());
    }

    @Test void unavailableApiUsesFallbackThenAutomaticallyAdoptsDownloadedLimits() {
        status = 503;
        var model = new io.agentscope.core.model.Model() {
            @Override public String getModelName() { return "a"; }
            @Override public reactor.core.publisher.Flux<io.agentscope.core.model.ChatResponse> stream(
                    List<io.agentscope.core.message.Msg> messages, List<io.agentscope.core.model.ToolSchema> tools,
                    io.agentscope.core.model.GenerateOptions options) { return reactor.core.publisher.Flux.empty(); }
        };
        ModelMetadata.register(model, "p", null, null, null);
        ModelContextResolver resolver = new ModelContextResolver(List.of(model), 524288, () -> catalog);
        ModelContextResolver.Context beforeRefresh = resolver.context(model);
        assertEquals(524288, resolver.limits(model).context());
        assertTrue(resolver.usesFallback(model));
        status = 200;
        clock.advance(Duration.ofHours(7));
        catalog.refresh(false);
        assertEquals(10000, resolver.limits(model).context());
        assertFalse(resolver.usesFallback(model));
        assertTrue(beforeRefresh.fallback());
        assertEquals(524288, beforeRefresh.limits().context());
    }

    @Test void corruptLocalSnapshotIsDownloadedAgainWithoutSendingItsEtag() throws Exception {
        Files.writeString(directory.resolve("models.json"), "{\"schema\":1,\"etag\":\"bad\",\"models\":[");
        assertEquals(10000, catalog.lookup("p", "a").orElseThrow().context());
        assertNull(receivedEtag);
        assertEquals(1, requests.get());
    }

    static String data(int context) {
        return "{\"p\":{\"pricing\":{},\"models\":{\"a\":{\"description\":\"SECRET-UNNEEDED-DESCRIPTION\","
                + "\"limit\":{\"context\":" + context + ",\"output\":1000}},"
                + "\"input-only\":{\"limit\":{\"input\":1234}},\"unknown\":{\"limit\":{\"context\":0}}}},"
                + "\"other-provider\":{\"models\":{\"a\":{\"limit\":{\"context\":20000}}}}}";
    }

    static class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2026-09-17T00:00:00Z");
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
