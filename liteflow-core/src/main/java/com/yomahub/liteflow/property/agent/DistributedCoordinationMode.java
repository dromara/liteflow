package com.yomahub.liteflow.property.agent;

/** Declaration of how deployments coordinate across JVM boundaries. */
public enum DistributedCoordinationMode {
	NONE,
	STICKY_ROUTING,
	DISTRIBUTED_GUARD
}
