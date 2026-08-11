package com.yomahub.liteflow.property.agent;

/** AgentScope Harness filesystem policy. */
public class HarnessConfig {

	private HarnessFilesystemBackend filesystemBackend = HarnessFilesystemBackend.GUARDED_LOCAL;
	private boolean trustedLocal;
	private DockerSandboxConfig docker = new DockerSandboxConfig();

	public HarnessFilesystemBackend getFilesystemBackend() {
		return filesystemBackend;
	}

	public void setFilesystemBackend(HarnessFilesystemBackend filesystemBackend) {
		this.filesystemBackend = filesystemBackend;
	}

	public boolean isTrustedLocal() {
		return trustedLocal;
	}

	public void setTrustedLocal(boolean trustedLocal) {
		this.trustedLocal = trustedLocal;
	}

	public DockerSandboxConfig getDocker() {
		return docker;
	}

	public void setDocker(DockerSandboxConfig docker) {
		this.docker = docker;
	}

	/** Validates the selected filesystem policy before a Harness runtime is built. */
	public void validate() {
		if (filesystemBackend == null) {
			throw new IllegalStateException(
					"liteflow.agent.harness.filesystem-backend must not be null");
		}
		if (filesystemBackend == HarnessFilesystemBackend.GUARDED_LOCAL && !trustedLocal) {
			throw new IllegalStateException(
					"liteflow.agent.harness.trusted-local must be true for GUARDED_LOCAL");
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
