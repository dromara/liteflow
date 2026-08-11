package com.yomahub.liteflow.property.agent;

import java.util.ArrayList;
import java.util.List;

/** Docker sandbox settings used by the optional AgentScope Harness module. */
public class DockerSandboxConfig {

	private String image = "ubuntu:22.04";
	private String workspaceRoot = "/workspace";
	private Long memorySizeBytes = 512L * 1024 * 1024;
	private Long cpuCount = 1L;
	private String network = "none";
	private String snapshotRoot;
	private boolean workspaceProjectionEnabled = true;
	private List<String> workspaceProjectionRoots = new ArrayList<>(List.of(
			"AGENTS.md", "skills", "subagents", "knowledge", ".skills-cache"));

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getWorkspaceRoot() {
		return workspaceRoot;
	}

	public void setWorkspaceRoot(String workspaceRoot) {
		this.workspaceRoot = workspaceRoot;
	}

	public Long getMemorySizeBytes() {
		return memorySizeBytes;
	}

	public void setMemorySizeBytes(Long memorySizeBytes) {
		this.memorySizeBytes = memorySizeBytes;
	}

	public Long getCpuCount() {
		return cpuCount;
	}

	public void setCpuCount(Long cpuCount) {
		this.cpuCount = cpuCount;
	}

	public String getNetwork() {
		return network;
	}

	public void setNetwork(String network) {
		this.network = network;
	}

	public String getSnapshotRoot() {
		return snapshotRoot;
	}

	public void setSnapshotRoot(String snapshotRoot) {
		this.snapshotRoot = snapshotRoot;
	}

	public boolean isWorkspaceProjectionEnabled() {
		return workspaceProjectionEnabled;
	}

	public void setWorkspaceProjectionEnabled(boolean workspaceProjectionEnabled) {
		this.workspaceProjectionEnabled = workspaceProjectionEnabled;
	}

	public List<String> getWorkspaceProjectionRoots() {
		return workspaceProjectionRoots;
	}

	public void setWorkspaceProjectionRoots(List<String> workspaceProjectionRoots) {
		this.workspaceProjectionRoots = workspaceProjectionRoots == null
				? null
				: new ArrayList<>(workspaceProjectionRoots);
	}

	/** Validates resource limits and paths before Docker options are constructed. */
	public void validate() {
		requireText(image, "image");
		requireText(workspaceRoot, "workspace-root");
		if (memorySizeBytes == null || memorySizeBytes <= 0) {
			throw invalid("memory-size-bytes", "must be positive");
		}
		if (cpuCount == null || cpuCount <= 0) {
			throw invalid("cpu-count", "must be positive");
		}
		requireText(network, "network");
		if (workspaceProjectionRoots == null) {
			throw invalid("workspace-projection-roots", "must not be null");
		}
		for (String root : workspaceProjectionRoots) {
			requireText(root, "workspace-projection-roots");
			for (String segment : root.split("[\\\\/]")) {
				if ("..".equals(segment)) {
					throw invalid(
							"workspace-projection-roots", "must not contain '..' segments");
				}
			}
		}
	}

	private static void requireText(String value, String property) {
		if (value == null || value.trim().isEmpty()) {
			throw invalid(property, "must not be blank");
		}
	}

	private static IllegalStateException invalid(String property, String requirement) {
		return new IllegalStateException(
				"liteflow.agent.harness.docker." + property + " " + requirement);
	}
}
