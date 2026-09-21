package com.yomahub.liteflow.property.agent;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Docker sandbox settings used by the optional AgentScope Harness module. */
public class DockerSandboxConfig {

	private String image = "ubuntu:22.04";
	private String workspaceRoot = "/workspace";
	private Long memorySizeBytes = 512L * 1024 * 1024;
	private Long cpuCount = 1L;
	private DockerNetworkMode network = DockerNetworkMode.NONE;
	private String snapshotRoot;
	private DockerSandboxLifecycle lifecycle = DockerSandboxLifecycle.PER_CALL;
	private Duration idleTimeout = Duration.ofMinutes(10);
	private Duration evictionInterval = Duration.ofSeconds(30);
	private int maxCachedSandboxes = 8;

	private boolean workspaceProjectionEnabled = true;
	private List<String> workspaceProjectionRoots = new ArrayList<>(Arrays.asList(
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

	public DockerNetworkMode getNetwork() {
		return network;
	}

	public void setNetwork(DockerNetworkMode network) {
		this.network = network;
	}

	public String getSnapshotRoot() {
		return snapshotRoot;
	}

	public void setSnapshotRoot(String snapshotRoot) {
		this.snapshotRoot = snapshotRoot;
	}

	public DockerSandboxLifecycle getLifecycle() {
		return lifecycle;
	}

	public void setLifecycle(DockerSandboxLifecycle lifecycle) {
		this.lifecycle = lifecycle;
	}

	public Duration getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(Duration idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public Duration getEvictionInterval() {
		return evictionInterval;
	}

	public void setEvictionInterval(Duration evictionInterval) {
		this.evictionInterval = evictionInterval;
	}

	public int getMaxCachedSandboxes() {
		return maxCachedSandboxes;
	}

	public void setMaxCachedSandboxes(int maxCachedSandboxes) {
		this.maxCachedSandboxes = maxCachedSandboxes;
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
		if (lifecycle == null) {
			throw invalid("lifecycle", "must not be null");
		}
		if (idleTimeout == null || idleTimeout.isNegative() || idleTimeout.isZero()) {
			throw invalid("idle-timeout", "must be positive");
		}
		if (evictionInterval == null || evictionInterval.isNegative() || evictionInterval.toMillis() < 1) {
			throw invalid("eviction-interval", "must be at least 1ms");
		}
		if (maxCachedSandboxes < 1) {
			throw invalid("max-cached-sandboxes", "must be positive");
		}
		requireText(image, "image");
		validateImageArgument(image);
		requireText(workspaceRoot, "workspace-root");
		if (memorySizeBytes == null || memorySizeBytes <= 0) {
			throw invalid("memory-size-bytes", "must be positive");
		}
		if (cpuCount == null || cpuCount <= 0) {
			throw invalid("cpu-count", "must be positive");
		}
		if (network == null) {
			throw invalid("network", "must not be null");
		}
		if (workspaceProjectionRoots == null) {
			throw invalid("workspace-projection-roots", "must not be null");
		}
		for (String root : workspaceProjectionRoots) {
			validateProjectionRoot(root);
		}
	}

	private static void validateImageArgument(String image) {
		String trimmed = image.trim();
		if (trimmed.startsWith("-")) {
			throw invalid("image", "must not start with '-' after trimming");
		}
		for (int index = 0; index < image.length(); index++) {
			char character = image.charAt(index);
			if (character <= 0x1f
					|| character == 0x7f
					|| Character.isWhitespace(character)) {
				throw invalid(
						"image",
						"must be one Docker image argument without whitespace or control characters");
			}
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
			Path normalized = Paths.get(root).normalize();
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
