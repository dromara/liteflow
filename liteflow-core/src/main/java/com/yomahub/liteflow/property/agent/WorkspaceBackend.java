package com.yomahub.liteflow.property.agent;

/** Workspace isolation boundary. */
public enum WorkspaceBackend {
	GUARDED_LOCAL,
	REMOTE_FILESYSTEM,
	CONTAINER_SANDBOX
}
