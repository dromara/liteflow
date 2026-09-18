package com.yomahub.liteflow.agent.context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Raw and safe identifiers for one agent invocation. */
public record AgentInvocationIdentity(
        String namespace,
        String conversationId,
        String agentKey,
        String runtimeSessionId,
        String agentNamespace,
        String storeSessionId) {

    public AgentInvocationIdentity {
        requirePathSegment(namespace, "applicationName");
        requirePathSegment(conversationId, "conversationId");
        requirePathSegment(agentKey, "agentKey");
        runtimeSessionId = conversationId;
        agentNamespace = "lf-" + hash(namespace, agentKey);
        storeSessionId = agentNamespace + "." + runtimeSessionId;
    }

    static void requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    /** Application names, conversation IDs and Agent memory keys are literal directory names. */
    public static String requirePathSegment(String value, String name) {
        requireValue(value, name);
        if (value.equals(".") || value.equals("..") || value.indexOf('/') >= 0
                || value.indexOf('\\') >= 0 || value.chars().anyMatch(Character::isISOControl)
                || value.chars().anyMatch(ch -> "<>:\"|?*".indexOf(ch) >= 0)
                || value.endsWith(".") || value.endsWith(" ")) {
            throw new IllegalArgumentException(name + " must be a plain directory name");
        }
        return value;
    }

    private static String hash(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
