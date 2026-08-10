package com.yomahub.liteflow.agent.context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Generates collision-resistant AgentScope runtime and store identifiers. */
public final class InvocationIdentityResolver {

    private final String namespace;

    public InvocationIdentityResolver(String namespace) {
        AgentInvocationIdentity.requireValue(namespace, "namespace");
        this.namespace = namespace;
    }

    public AgentInvocationIdentity resolve(String userId, String conversationId, String agentKey) {
        AgentInvocationIdentity.requireValue(userId, "userId");
        AgentInvocationIdentity.requireValue(conversationId, "conversationId");
        AgentInvocationIdentity.requireValue(agentKey, "agentKey");

        String runtimeSessionId = "lf-" + hash(namespace, userId, conversationId);
        String agentNamespace = "lf-" + hash(namespace, agentKey);
        return new AgentInvocationIdentity(namespace, userId, conversationId, agentKey,
                runtimeSessionId, agentNamespace, agentNamespace + "." + runtimeSessionId);
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
