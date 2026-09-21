package com.yomahub.liteflow.agent.message;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/** Selects the AgentScope call overload and reply extraction mode for one invocation. */
public record AgentOutputSpec(Kind kind, Class<?> javaType, JsonNode jsonSchema) {

    public enum Kind {
        TEXT,
        JAVA_TYPE,
        JSON_SCHEMA
    }

    public AgentOutputSpec {
        Objects.requireNonNull(kind, "kind");
        switch (kind) {
            case TEXT -> {
                if (javaType != null || jsonSchema != null) {
                    throw new IllegalArgumentException("TEXT output cannot declare a type or schema");
                }
            }
            case JAVA_TYPE -> {
                Objects.requireNonNull(javaType, "javaType");
                if (jsonSchema != null) {
                    throw new IllegalArgumentException("JAVA_TYPE output cannot declare a schema");
                }
            }
            case JSON_SCHEMA -> {
                Objects.requireNonNull(jsonSchema, "jsonSchema");
                if (javaType != null) {
                    throw new IllegalArgumentException("JSON_SCHEMA output cannot declare a Java type");
                }
                jsonSchema = jsonSchema.deepCopy();
            }
        }
    }

    public static AgentOutputSpec text() {
        return new AgentOutputSpec(Kind.TEXT, null, null);
    }

    public static AgentOutputSpec javaType(Class<?> type) {
        return new AgentOutputSpec(Kind.JAVA_TYPE, type, null);
    }

    public static AgentOutputSpec jsonSchema(JsonNode schema) {
        return new AgentOutputSpec(Kind.JSON_SCHEMA, null, schema);
    }

    @Override
    public JsonNode jsonSchema() {
        return jsonSchema == null ? null : jsonSchema.deepCopy();
    }
}
