package com.yomahub.liteflow.property.agent;

import java.time.Duration;

/** Per-call memory extraction settings; session persistence is independent of this policy. */
public class HarnessMemoryConfig {

	/** Per-call extraction is opt-in, independently of the SDK's defaults. */
	private HarnessMemoryFlushMode flushMode = HarnessMemoryFlushMode.NEVER;
	private Duration flushMinGap = Duration.ofMinutes(5);

	public HarnessMemoryFlushMode getFlushMode() {
		return flushMode;
	}

	public void setFlushMode(HarnessMemoryFlushMode flushMode) {
		this.flushMode = flushMode;
	}

	public Duration getFlushMinGap() {
		return flushMinGap;
	}

	public void setFlushMinGap(Duration flushMinGap) {
		this.flushMinGap = flushMinGap;
	}

	public void validate() {
		if (flushMode == null) {
			throw new IllegalStateException("liteflow.agent.harness.memory.flush-mode must not be null");
		}
		if (flushMode == HarnessMemoryFlushMode.THROTTLED
				&& (flushMinGap == null || flushMinGap.isZero() || flushMinGap.isNegative())) {
			throw new IllegalStateException(
					"liteflow.agent.harness.memory.flush-min-gap must be positive for THROTTLED");
		}
	}
}
