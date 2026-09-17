package com.yomahub.liteflow.property.agent;

/** AgentScope Harness filesystem, Shell and memory policy. */
public class HarnessConfig {

	private HarnessFilesystemBackend filesystemBackend = HarnessFilesystemBackend.GUARDED_LOCAL;
	private DockerSandboxConfig docker = new DockerSandboxConfig();
	private HarnessMemoryConfig memory = new HarnessMemoryConfig();
	private LocalWorkspaceConfig local = new LocalWorkspaceConfig();
	private ShellConfig shell = new ShellConfig();
	private double compactionThreshold = 0.8;
	private int compactionFallbackContextWindow = 512 * 1024;
	private double compactionFallbackThreshold = 0.9;

	/** Fraction of the usable input budget at which conversation compaction starts. */
	public double getCompactionThreshold() { return compactionThreshold; }
	public void setCompactionThreshold(double value) { this.compactionThreshold = value; }
	public int getCompactionFallbackContextWindow() { return compactionFallbackContextWindow; }
	public void setCompactionFallbackContextWindow(int value) { this.compactionFallbackContextWindow = value; }
	public double getCompactionFallbackThreshold() { return compactionFallbackThreshold; }
	public void setCompactionFallbackThreshold(double value) { this.compactionFallbackThreshold = value; }

	public LocalWorkspaceConfig getLocal() { return local; }
	public void setLocal(LocalWorkspaceConfig local) { this.local = local; }

	public ShellConfig getShell() {
		return shell;
	}

	public void setShell(ShellConfig shell) {
		this.shell = shell;
	}

	public HarnessFilesystemBackend getFilesystemBackend() {
		return filesystemBackend;
	}

	public void setFilesystemBackend(HarnessFilesystemBackend filesystemBackend) {
		this.filesystemBackend = filesystemBackend;
	}

	public DockerSandboxConfig getDocker() {
		return docker;
	}

	public void setDocker(DockerSandboxConfig docker) {
		this.docker = docker;
	}

	public HarnessMemoryConfig getMemory() {
		return memory;
	}

	public void setMemory(HarnessMemoryConfig memory) {
		this.memory = memory;
	}

	/** Validates the selected filesystem policy before a Harness runtime is built. */
	public void validate() {
		if (!Double.isFinite(compactionThreshold) || compactionThreshold <= 0 || compactionThreshold >= 1) {
			throw new IllegalStateException("liteflow.agent.harness.compaction-threshold must be between 0 and 1 (exclusive)");
		}
		if (compactionFallbackContextWindow <= 0) {
			throw new IllegalStateException("liteflow.agent.harness.compaction-fallback-context-window must be positive");
		}
		if (!Double.isFinite(compactionFallbackThreshold) || compactionFallbackThreshold <= 0 || compactionFallbackThreshold >= 1) {
			throw new IllegalStateException("liteflow.agent.harness.compaction-fallback-threshold must be between 0 and 1 (exclusive)");
		}
        if (filesystemBackend == HarnessFilesystemBackend.GUARDED_LOCAL && local == null) {
            throw new IllegalStateException("liteflow.agent.harness.local must not be null");
        }
		if (memory == null) {
			throw new IllegalStateException("liteflow.agent.harness.memory must not be null");
		}
		memory.validate();
		if (filesystemBackend == null) {
			throw new IllegalStateException(
					"liteflow.agent.harness.filesystem-backend must not be null");
		}
		if (filesystemBackend == HarnessFilesystemBackend.DOCKER) {
			if (docker == null) {
				throw new IllegalStateException(
						"liteflow.agent.harness.docker must not be null for DOCKER");
			}
			docker.validate();
		}
	}
}
