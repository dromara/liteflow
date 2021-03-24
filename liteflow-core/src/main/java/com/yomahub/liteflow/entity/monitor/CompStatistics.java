/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.monitor;

/**
 * 统计类
 * @author Bryan.Zhang
 */
public class CompStatistics implements Comparable{

	private String componentClazzName;

	private long timeSpent;

	private long memorySpent;

	public String getComponentClazzName() {
		return componentClazzName;
	}

	public void setComponentClazzName(String componentClazzName) {
		this.componentClazzName = componentClazzName;
	}

	public long getTimeSpent() {
		return timeSpent;
	}

	public void setTimeSpent(long timeSpent) {
		this.timeSpent = timeSpent;
	}

	public long getMemorySpent() {
		return memorySpent;
	}

	public void setMemorySpent(long memorySpent) {
		this.memorySpent = memorySpent;
	}

	@Override
	public int compareTo(Object o) {
		return -1;
	}
}
