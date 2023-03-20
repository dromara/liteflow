package com.yomahub.liteflow.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 监控器的基础参数类
 *
 * @author Bryan.Zhang
 */
@ConfigurationProperties(prefix = "liteflow.monitor", ignoreUnknownFields = true)
public class LiteflowMonitorProperty {

	// 是否打印监控日志
	private boolean enableLog;

	// 监控队列存储的最大数量
	private int queueLimit;

	// 延迟多少毫秒打印
	private long delay;

	// 每隔多少毫秒打印
	private long period;

	public boolean isEnableLog() {
		return enableLog;
	}

	public void setEnableLog(boolean enableLog) {
		this.enableLog = enableLog;
	}

	public int getQueueLimit() {
		return queueLimit;
	}

	public void setQueueLimit(int queueLimit) {
		this.queueLimit = queueLimit;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public long getPeriod() {
		return period;
	}

	public void setPeriod(long period) {
		this.period = period;
	}

}
