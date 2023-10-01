/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.element.condition;

import com.yomahub.liteflow.common.LocalDefaultFlowConstant;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.enums.ParallelStrategyEnum;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.parallel.strategy.ParallelStrategyExecutor;
import com.yomahub.liteflow.flow.parallel.strategy.ParallelStrategyHelper;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.thread.ExecutorHelper;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 并行器
 *
 * @author Bryan.Zhang
 */
public class WhenCondition extends Condition {

	private final LFLog LOG = LFLoggerManager.getLogger(this.getClass());

	// 只在when类型下有效，以区分当when调用链调用失败时是否继续往下执行 默认false不继续执行
	private boolean ignoreError = false;

	// 只在when类型下有效，用于不同node进行同组合并，相同的组会进行合并，不同的组不会进行合并
	// 此属性已弃用
	private String group = LocalDefaultFlowConstant.DEFAULT;

	// 当前 When 对应并行策略，默认为 ALL
	private ParallelStrategyEnum parallelStrategy;

	// 只有 must 条件下，才会赋值 specifyIdSet
	private Set<String> specifyIdSet;

	// when单独的线程池名称
	private String threadExecutorClass;

	// 异步线程最⻓的等待时间
	private Integer maxWaitTime;

	// 等待时间单位
	private TimeUnit maxWaitTimeUnit;

	@Override
	public void executeCondition(Integer slotIndex) throws Exception {
		executeAsyncCondition(slotIndex);
	}

	@Override
	public ConditionTypeEnum getConditionType() {
		return ConditionTypeEnum.TYPE_WHEN;
	}

	// 使用线程池执行 when 并发流程
	// 这块涉及到挺多的多线程逻辑，所以注释比较详细，看到这里的童鞋可以仔细阅读
	private void executeAsyncCondition(Integer slotIndex) throws Exception {

		// 获取并发执行策略
		ParallelStrategyExecutor parallelStrategyExecutor = ParallelStrategyHelper.loadInstance().buildParallelExecutor(this.getParallelStrategy());

		// 执行并发逻辑
		parallelStrategyExecutor.execute(this, slotIndex);

	}

	public boolean isIgnoreError() {
		return ignoreError;
	}

	public void setIgnoreError(boolean ignoreError) {
		this.ignoreError = ignoreError;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public ParallelStrategyEnum getParallelStrategy() {
		return parallelStrategy;
	}

	public void setParallelStrategy(ParallelStrategyEnum parallelStrategy) {
		this.parallelStrategy = parallelStrategy;
	}

	public Set<String> getSpecifyIdSet() {
		return specifyIdSet;
	}

	public void setSpecifyIdSet(Set<String> specifyIdSet) {
		this.specifyIdSet = specifyIdSet;
	}

	public String getThreadExecutorClass() {
		return threadExecutorClass;
	}

	public void setThreadExecutorClass(String threadExecutorClass) {
		// #I7G6BB 初始化的时候即创建线程池，避免运行时获取导致并发问题
		ExecutorHelper.loadInstance().buildWhenExecutor(threadExecutorClass);
		this.threadExecutorClass = threadExecutorClass;
	}

	public Integer getMaxWaitTime() {
		return maxWaitTime;
	}

	public void setMaxWaitTime(Integer maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}

	public TimeUnit getMaxWaitTimeUnit() {
		return maxWaitTimeUnit;
	}

	public void setMaxWaitTimeUnit(TimeUnit maxWaitTimeUnit) {
		this.maxWaitTimeUnit = maxWaitTimeUnit;
	}
}
