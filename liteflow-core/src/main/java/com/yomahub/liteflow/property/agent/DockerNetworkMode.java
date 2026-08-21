package com.yomahub.liteflow.property.agent;

/** Network isolation mode for the Docker sandbox. */
public enum DockerNetworkMode {

	NONE("none"),
	BRIDGE("bridge"),
	HOST("host");

	private final String dockerValue;

	DockerNetworkMode(String dockerValue) {
		this.dockerValue = dockerValue;
	}

	public String getDockerValue() {
		return dockerValue;
	}
}
