package com.yomahub.liteflow.property.agent;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
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
			validateProjectionRoot(root);
		}
	}

	private static void validateProjectionRoot(String root) {
		requireText(root, "workspace-projection-roots");
		if (root.startsWith("/") || root.startsWith("\\") || hasWindowsDrivePrefix(root)) {
			throw invalid("workspace-projection-roots", "must contain only relative paths");
		}

		for (String segment : root.split("[\\\\/]+")) {
			if ("..".equals(segment)) {
				throw invalid(
						"workspace-projection-roots", "must not contain '..' segments");
			}
			if (".".equals(segment)) {
				continue;
			}
			String canonicalSegment = stripWindowsTrailingDotsAndSpaces(segment);
			if (canonicalSegment.isEmpty()
					|| ".".equals(canonicalSegment)
					|| "..".equals(canonicalSegment)) {
				throw invalid(
						"workspace-projection-roots",
						"must not contain segments that Windows normalizes to empty or traversal paths");
			}
		}

		try {
			Path normalized = Path.of(root).normalize();
			if (normalized.isAbsolute()) {
				throw invalid("workspace-projection-roots", "must contain only relative paths");
			}
		}
		catch (InvalidPathException failure) {
			throw invalid("workspace-projection-roots", "must contain valid paths");
		}
	}

	private static String stripWindowsTrailingDotsAndSpaces(String segment) {
		int end = segment.length();
		while (end > 0) {
			char trailing = segment.charAt(end - 1);
			if (trailing != ' ' && trailing != '.') {
				break;
			}
			end--;
		}
		return segment.substring(0, end);
	}

	private static boolean hasWindowsDrivePrefix(String root) {
		if (root.length() < 2 || root.charAt(1) != ':') {
			return false;
		}
		char drive = root.charAt(0);
		return (drive >= 'A' && drive <= 'Z') || (drive >= 'a' && drive <= 'z');
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
