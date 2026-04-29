package com.yomahub.liteflow.property.agent;

/**
 * Settings that only apply when {@link MemoryStorageMode#WORKSPACE_FILE} is selected.
 *
 * <p>The persistence sub-directory is hard-coded to {@code .agent-session} so it
 * stays out of the way of tool-produced files in the per-session workspace and
 * is not configurable on purpose; users who want a custom location can plug in
 * their own {@code AgentSessionFactory} via SPI.
 */
public class WorkspaceMemoryConfig {
    /** Fixed sub-directory created under the workspace root that holds session JSON files. */
    public static final String SUB_DIR = ".agent-session";
}
