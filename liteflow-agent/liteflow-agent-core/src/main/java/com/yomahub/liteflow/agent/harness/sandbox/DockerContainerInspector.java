package com.yomahub.liteflow.agent.harness.sandbox;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.yomahub.liteflow.agent.harness.sandbox.AgentSandboxStatus.State;

/** Uses the same Docker CLI/context as AgentScope; never starts or resumes a container. */
final class DockerContainerInspector {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String FORMAT = "{\"id\":{{json .Id}},\"name\":{{json .Name}},"
            + "\"image\":{{json .Config.Image}},\"status\":{{json .State.Status}}}";

    record Inspection(State state, String id, String name, String image, String message) {
        static Inspection unknown(String message) { return new Inspection(State.UNKNOWN, null, null, null, message); }
    }

    Inspection inspect(String containerId) {
        Process process = null;
        try {
            process = new ProcessBuilder("docker", "inspect", "--type", "container", "--format", FORMAT,
                    "--", containerId).start();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                return Inspection.unknown("Docker status query timed out");
            }
            // The format deliberately excludes environment, mounts and other private container data.
            String output = new String(process.getInputStream().readNBytes(8192), StandardCharsets.UTF_8);
            String error = new String(process.getErrorStream().readNBytes(8192), StandardCharsets.UTF_8);
            return parse(process.exitValue(), output, error);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            return Inspection.unknown("Docker status query interrupted");
        } catch (IOException failure) {
            return Inspection.unknown("Docker is unavailable");
        } finally {
            if (process != null) {
                if (process.isAlive()) process.destroyForcibly();
                try { process.getInputStream().close(); } catch (IOException ignored) { }
                try { process.getErrorStream().close(); } catch (IOException ignored) { }
                try { process.getOutputStream().close(); } catch (IOException ignored) { }
            }
        }
    }

    static Inspection parse(int exitCode, String output, String error) {
        if (exitCode != 0) {
            String lower = error.toLowerCase(Locale.ROOT);
            if (lower.contains("no such object:") || lower.contains("no such container:")) {
                return new Inspection(State.NOT_FOUND, null, null, null, "Container no longer exists");
            }
            return Inspection.unknown("Docker status query failed");
        }
        try {
            var data = JSON.readTree(output);
            String id = data.path("id").asText();
            if (id.isBlank()) return Inspection.unknown("Invalid Docker status response");
            State state = switch (data.path("status").asText()) {
                case "created" -> State.CREATED;
                case "running" -> State.RUNNING;
                case "exited" -> State.STOPPED;
                case "paused" -> State.PAUSED;
                case "restarting" -> State.RESTARTING;
                case "removing" -> State.REMOVING;
                case "dead" -> State.DEAD;
                default -> State.UNKNOWN;
            };
            return new Inspection(state, id, data.path("name").asText().replaceFirst("^/", ""),
                    data.path("image").asText(), state == State.UNKNOWN ? "Unknown Docker state" : null);
        } catch (IOException | RuntimeException failure) {
            return Inspection.unknown("Invalid Docker status response");
        }
    }
}
