package com.yomahub.liteflow.agent.model.catalog;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * Shared, refreshable disk catalog. Streaming projection retains no upstream object tree;
 * only recently requested model limits (including misses) occupy the bounded heap cache.
 * No credentials, prompts or conversation data are sent to the catalog service.
 */
public final class ModelsDevCatalog implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ModelsDevCatalog.class);
    private static final JsonFactory JSON = JsonFactory.builder()
            .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
            .disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES).build();
    private static final int MAX_ENTRIES = 256;
    private static final int SCHEMA = 1;
    private final Path file;
    private final URI source;
    private final Duration refreshInterval;
    private final Duration retryInterval;
    private final Clock clock;
    private final Object snapshotLock = new Object();
    private final Object refreshLock = new Object();
    private final Map<Key, Optional<ModelLimits>> cache = new LinkedHashMap<>(16, .75f, true);
    private final ScheduledExecutorService scheduler;
    private volatile boolean initialized;
    private volatile boolean available;
    private volatile boolean closed;
    private volatile long checkedAt;
    private long attemptedAt = Long.MIN_VALUE;
    private volatile String etag;

    private static final class Shared {
        private static final ModelsDevCatalog INSTANCE = new ModelsDevCatalog(
                Path.of(System.getProperty("liteflow.agent.model-catalog.cache-dir",
                        Path.of(System.getProperty("user.home"), ".liteflow", "model-catalog").toString()))
                        .resolve("models-v1.json"),
                URI.create("https://models.dev/api.json"), Duration.ofHours(6),
                Duration.ofMinutes(5), Clock.systemUTC());
    }

    public static ModelsDevCatalog shared() {
        return Shared.INSTANCE;
    }

    ModelsDevCatalog(Path file, URI source, Duration refreshInterval, Duration retryInterval, Clock clock) {
        this.file = file;
        this.source = source;
        this.refreshInterval = refreshInterval;
        this.retryInterval = retryInterval;
        this.clock = clock;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "liteflow-model-catalog");
            thread.setDaemon(true);
            thread.setContextClassLoader(null);
            return thread;
        });
    }

    /** Called during model initialization, never on the ordinary per-token execution path. */
    public Optional<ModelLimits> lookup(String provider, String model) {
        if (provider == null || provider.isBlank() || model == null || model.isBlank()) {
            return Optional.empty();
        }
        initialize();
        Key key = new Key(provider, model);
        synchronized (snapshotLock) {
            Optional<ModelLimits> hit = cache.get(key);
            if (hit != null && hit.isPresent()) {
                return hit;
            }
        }
        Optional<ModelLimits> result = readAndCache(key);
        if (result.isEmpty()) {
            // Global cooldown and one refresh lock coalesce simultaneous unknown-model lookups.
            refresh(true);
            result = readAndCache(key);
        }
        return result;
    }

    /** Fast lookup for already prepared models; a refresh updates these entries in the background. */
    public Optional<ModelLimits> cached(String provider, String model) {
        synchronized (snapshotLock) {
            return cache.getOrDefault(new Key(provider, model), Optional.empty());
        }
    }

    /** Cache hits, including known misses, are memory-only. An evicted entry is loaded once again. */
    public Optional<ModelLimits> current(String provider, String model) {
        synchronized (snapshotLock) {
            Optional<ModelLimits> hit = cache.get(new Key(provider, model));
            if (hit != null) return hit;
        }
        return lookup(provider, model);
    }

    private void initialize() {
        if (initialized || closed) return;
        synchronized (refreshLock) {
            if (initialized || closed) return;
            try {
                etag = readEtag();
                checkedAt = Files.getLastModifiedTime(file).toMillis();
                available = true;
            } catch (IOException failure) {
                available = false;
            }
            if (!available) refresh(true);
            initialized = true;
            scheduler.scheduleWithFixedDelay(() -> refresh(false),
                    available ? Math.max(0, refreshInterval.toMillis() - (clock.millis() - checkedAt))
                            : retryInterval.toMillis(),
                    retryInterval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private Optional<ModelLimits> readAndCache(Key key) {
        synchronized (snapshotLock) {
            Optional<ModelLimits> hit = cache.get(key);
            if (hit != null) return hit;
            Optional<ModelLimits> result = Optional.empty();
            if (available) {
                try {
                    result = readRecord(key);
                } catch (IOException failure) {
                    available = false;
                    etag = null;
                    LOG.warn("Cannot read model catalog cache: {}", failure.getMessage());
                }
            }
            cache.put(key, result);
            while (cache.size() > MAX_ENTRIES) cache.remove(cache.keySet().iterator().next());
            return result;
        }
    }

    /** Conditional GET. A 200 replaces a complete snapshot; a 304 has no response body. */
    void refresh(boolean requestedByMiss) {
        synchronized (refreshLock) {
            long now = clock.millis();
            if (closed || (attemptedAt != Long.MIN_VALUE && now - attemptedAt < retryInterval.toMillis())) return;
            if (!requestedByMiss && available && now - checkedAt < refreshInterval.toMillis()) return;
            attemptedAt = now;
            HttpURLConnection connection = null;
            Path temporary = null;
            try {
                connection = (HttpURLConnection) source.toURL().openConnection();
                connection.setConnectTimeout(3_000);
                connection.setReadTimeout(5_000);
                connection.setRequestProperty("Accept-Encoding", "gzip");
                if (available && etag != null) connection.setRequestProperty("If-None-Match", etag);
                int status = connection.getResponseCode();
                if (status == HttpURLConnection.HTTP_NOT_MODIFIED && available && Files.isRegularFile(file)) {
                    Files.setLastModifiedTime(file, FileTime.fromMillis(now));
                    checkedAt = now;
                    return;
                }
                if (status != HttpURLConnection.HTTP_OK) throw new IOException("Catalog HTTP status " + status);
                Files.createDirectories(file.toAbsolutePath().getParent());
                temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "models-", ".tmp");
                String newEtag = connection.getHeaderField("ETag");
                try (InputStream raw = connection.getInputStream();
                        InputStream input = "gzip".equalsIgnoreCase(connection.getContentEncoding())
                                ? new GZIPInputStream(raw) : raw) {
                    project(input, temporary, newEtag);
                }
                synchronized (snapshotLock) {
                    try {
                        Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    } catch (AtomicMoveNotSupportedException failure) {
                        // Keep the previous complete snapshot on filesystems without atomic replacement.
                        throw new IOException("Model catalog cache requires atomic file replacement", failure);
                    }
                    etag = newEtag;
                    available = true;
                    checkedAt = now;
                    Files.setLastModifiedTime(file, FileTime.fromMillis(now));
                    // Reload only the bounded set of active keys, so ordinary model calls stay in memory.
                    if (!cache.isEmpty()) {
                        Map<Key, Optional<ModelLimits>> refreshed = new java.util.HashMap<>();
                        cache.keySet().forEach(key -> refreshed.put(key, Optional.empty()));
                        readRecords(null, refreshed);
                        cache.replaceAll((key, previous) -> refreshed.get(key));
                    }
                }
            } catch (IOException | RuntimeException failure) {
                LOG.warn("Model catalog refresh failed; retaining last available limits: {}", failure.getMessage());
            } finally {
                if (connection != null) connection.disconnect();
                if (temporary != null) {
                    try { Files.deleteIfExists(temporary); }
                    catch (IOException failure) { LOG.debug("Cannot remove catalog temporary file", failure); }
                }
            }
        }
    }

    private String readEtag() throws IOException {
        try (JsonParser parser = JSON.createParser(file.toFile())) {
            require(parser.nextToken(), JsonToken.START_OBJECT);
            require(parser.nextToken(), JsonToken.FIELD_NAME);
            if (!"schema".equals(parser.currentName()) || parser.nextIntValue(-1) != SCHEMA) {
                throw new IOException("Unsupported model catalog cache schema");
            }
            require(parser.nextToken(), JsonToken.FIELD_NAME);
            if (!"etag".equals(parser.currentName())) throw new IOException("Missing catalog ETag field");
            parser.nextToken();
            String result = parser.currentToken() == JsonToken.VALUE_NULL ? null : parser.getText();
            require(parser.nextToken(), JsonToken.FIELD_NAME);
            if (!"models".equals(parser.currentName())) throw new IOException("Missing model catalog records");
            require(parser.nextToken(), JsonToken.START_ARRAY);
            parser.skipChildren(); // Validate complete JSON once on restart, without retaining the records.
            require(parser.nextToken(), JsonToken.END_OBJECT);
            if (parser.nextToken() != null) throw new IOException("Trailing model catalog data");
            return result;
        }
    }

    private Optional<ModelLimits> readRecord(Key wanted) throws IOException {
        return readRecords(wanted, null);
    }

    private Optional<ModelLimits> readRecords(Key wanted, Map<Key, Optional<ModelLimits>> refreshed) throws IOException {
        try (JsonParser parser = JSON.createParser(file.toFile())) {
            require(parser.nextToken(), JsonToken.START_OBJECT);
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                require(parser.currentToken(), JsonToken.FIELD_NAME);
                String field = parser.currentName();
                parser.nextToken();
                if (!"models".equals(field)) { parser.skipChildren(); continue; }
                require(parser.currentToken(), JsonToken.START_ARRAY);
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    require(parser.currentToken(), JsonToken.START_ARRAY);
                    String provider = parser.nextTextValue();
                    String model = parser.nextTextValue();
                    Integer context = positive(parser);
                    Integer input = positive(parser);
                    Integer output = positive(parser);
                    require(parser.nextToken(), JsonToken.END_ARRAY);
                    if (wanted != null && wanted.provider.equals(provider) && wanted.model.equals(model)) {
                        return Optional.of(new ModelLimits(context, input, output));
                    }
                    if (refreshed != null) {
                        Key key = new Key(provider, model);
                        if (refreshed.containsKey(key)) refreshed.put(key, Optional.of(new ModelLimits(context, input, output)));
                    }
                }
            }
            return Optional.empty();
        }
    }

    /** Converts upstream data to [provider, model, context, input, output] records in one pass. */
    private static void project(InputStream input, Path target, String etag) throws IOException {
        try (JsonParser parser = JSON.createParser(input);
                JsonGenerator writer = JSON.createGenerator(target.toFile(), com.fasterxml.jackson.core.JsonEncoding.UTF8)) {
            writer.writeStartObject();
            writer.writeNumberField("schema", SCHEMA);
            writer.writeStringField("etag", etag);
            writer.writeArrayFieldStart("models");
            require(parser.nextToken(), JsonToken.START_OBJECT);
            int records = 0;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                require(parser.currentToken(), JsonToken.FIELD_NAME);
                String provider = parser.currentName();
                require(parser.nextToken(), JsonToken.START_OBJECT);
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    require(parser.currentToken(), JsonToken.FIELD_NAME);
                    String field = parser.currentName();
                    parser.nextToken();
                    if (!"models".equals(field)) { parser.skipChildren(); continue; }
                    require(parser.currentToken(), JsonToken.START_OBJECT);
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        require(parser.currentToken(), JsonToken.FIELD_NAME);
                        String model = parser.currentName();
                        require(parser.nextToken(), JsonToken.START_OBJECT);
                        Integer context = null, maxInput = null, output = null;
                        while (parser.nextToken() != JsonToken.END_OBJECT) {
                            require(parser.currentToken(), JsonToken.FIELD_NAME);
                            String property = parser.currentName();
                            parser.nextToken();
                            if (!"limit".equals(property)) { parser.skipChildren(); continue; }
                            require(parser.currentToken(), JsonToken.START_OBJECT);
                            while (parser.nextToken() != JsonToken.END_OBJECT) {
                                require(parser.currentToken(), JsonToken.FIELD_NAME);
                                String limit = parser.currentName();
                                Integer value = positive(parser);
                                switch (limit) {
                                    case "context" -> context = value;
                                    case "input" -> maxInput = value;
                                    case "output" -> output = value;
                                    default -> { }
                                }
                            }
                        }
                        if (context == null && maxInput == null) continue;
                        writer.writeStartArray();
                        writer.writeString(provider);
                        writer.writeString(model);
                        writeLimit(writer, context);
                        writeLimit(writer, maxInput);
                        writeLimit(writer, output);
                        writer.writeEndArray();
                        records++;
                    }
                }
            }
            if (records == 0 || parser.nextToken() != null) throw new IOException("Empty or invalid model catalog");
            writer.writeEndArray();
            writer.writeEndObject();
        }
    }

    private static Integer positive(JsonParser parser) throws IOException {
        JsonToken token = parser.nextToken();
        if (token == JsonToken.VALUE_NULL) return null;
        if (token != JsonToken.VALUE_NUMBER_INT) { parser.skipChildren(); return null; }
        long value = parser.getLongValue();
        return value > 0 && value <= Integer.MAX_VALUE ? (int) value : null;
    }

    private static void writeLimit(JsonGenerator writer, Integer value) throws IOException {
        if (value == null) writer.writeNull(); else writer.writeNumber(value);
    }

    private static void require(JsonToken actual, JsonToken expected) throws IOException {
        if (actual != expected) throw new IOException("Invalid model catalog: expected " + expected);
    }

    int cachedEntries() { synchronized (snapshotLock) { return cache.size(); } }

    @Override
    public void close() {
        closed = true;
        scheduler.shutdownNow();
    }

    private record Key(String provider, String model) { }
}
