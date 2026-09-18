package com.yomahub.liteflow.property.agent;

import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;

/** Guide 7: independent, explicit expected defaults, including properties absent from binding fixtures. */
class AgentGuideDefaultsTest {
    @Test void guideDefaultsRemainStable() throws Exception {
        AgentConfig config = new AgentConfig();
        Map<String, Object> expected = Map.ofEntries(
                entry("executionTimeout", Duration.ofMinutes(10)), entry("maxIterations", 100),
                entry("executionLogEnabled", true), entry("conversationHistoryEnabled", true),
                entry("event.listenerFailureMode", AgentListenerFailureMode.FAIL_FAST),
                entry("sessionStore.type", AgentSessionStoreType.JSON), entry("sessionStore.jsonRoot", "./data/agent-state"),
                entry("sessionStore.failurePolicy", AgentSessionStoreFailurePolicy.FAIL_FAST),
                entry("sessionStore.mysql.createIfNotExist", false),
                entry("harness.filesystemBackend", HarnessFilesystemBackend.GUARDED_LOCAL),
                entry("harness.shell.mode", ShellMode.WHITELIST), entry("harness.shell.timeout", Duration.ofMinutes(1)),
                entry("harness.docker.image", "ubuntu:22.04"), entry("harness.docker.workspaceRoot", "/workspace"),
                entry("harness.docker.memorySizeBytes", 536870912L), entry("harness.docker.cpuCount", 1L),
                entry("harness.docker.network", DockerNetworkMode.NONE), entry("harness.docker.lifecycle", DockerSandboxLifecycle.PER_CALL),
                entry("harness.docker.idleTimeout", Duration.ofMinutes(10)), entry("harness.docker.evictionInterval", Duration.ofSeconds(30)),
                entry("harness.docker.maxCachedSandboxes", 8), entry("harness.docker.workspaceProjectionEnabled", true),
                entry("harness.docker.workspaceProjectionRoots", List.of("AGENTS.md", "skills", "subagents", "knowledge", ".skills-cache")),
                entry("skills.enabled", false), entry("skills.path", "./skills"), entry("skills.strict", true),
                entry("harness.compactionThreshold", .8), entry("harness.compactionFallbackContextWindow", 524288),
                entry("harness.compactionFallbackThreshold", .9), entry("harness.memory.flushMode", HarnessMemoryFlushMode.NEVER),
                entry("harness.memory.flushMinGap", Duration.ofMinutes(5)), entry("hitl.confirmationTimeout", Duration.ofMinutes(2)),
                entry("hitl.failOnDeniedTool", false), entry("toolkit.parallel", false),
                entry("invocationGuard.mode", AgentInvocationGuardMode.AUTO), entry("invocationGuard.acquireTimeout", Duration.ofMinutes(2)),
                entry("invocationGuard.leaseDuration", Duration.ofMinutes(2)));
        for (var property : expected.entrySet()) assertEquals(property.getValue(), read(config, property.getKey()), property.getKey());
        // These nullable properties delegate their effective defaults to the storage adapter;
        // MysqlBeanAndSchemaTest and RedisConversationServiceTest verify those defaults in use.
        for (String unset : List.of("applicationName", "sessionStore.jsonWorkspaceRoot", "sessionStore.redis.uri",
                "sessionStore.redis.clientBeanName", "sessionStore.redis.keyPrefix", "sessionStore.mysql.jdbcUrl",
                "sessionStore.mysql.databaseName", "sessionStore.mysql.tableName",
                "sessionStore.mysql.username", "sessionStore.mysql.password", "sessionStore.mysql.dataSourceBeanName",
                "harness.local.workspaceRoot", "harness.docker.snapshotRoot", "invocationGuard.beanName")) {
            assertNull(read(config, unset), unset);
        }
        assertEquals(List.of(("sh bash python python3 node git npm npx java javac mvn gradle "
                + "mkdir cp mv rm touch chmod ls find tree stat file basename dirname pwd which "
                + "cat head tail grep sed awk wc sort uniq cut tr diff echo printf expr "
                + "date whoami hostname uname env df du ps md5sum sha256sum jq curl wget").split(" ")),
                config.getHarness().getShell().getWhitelist());
    }

    private static Object read(Object bean, String path) throws Exception {
        Object current = bean;
        for (String part : path.split("\\.")) {
            var property = Arrays.stream(Introspector.getBeanInfo(current.getClass()).getPropertyDescriptors())
                    .filter(candidate -> candidate.getName().equals(part)).findFirst().orElseThrow();
            current = property.getReadMethod().invoke(current);
        }
        return current;
    }
}
