/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.common.LocalDefaultFlowConstant;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.WhenExecuteException;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.parallel.CompletableFutureTimeout;
import com.yomahub.liteflow.flow.parallel.ParallelSupplier;
import com.yomahub.liteflow.flow.parallel.WhenFutureObj;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.thread.ExecutorHelper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

	// 只在when类型下有效，为true的话说明在多个并行节点下，任意一个成功，整个when就成功
	private boolean any = false;

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

	// 使用线程池执行when并发流程
	// 这块涉及到挺多的多线程逻辑，所以注释比较详细，看到这里的童鞋可以仔细阅读
	private void executeAsyncCondition(Integer slotIndex) throws Exception {
		Slot slot = DataBus.getSlot(slotIndex);

		String currChainName = this.getCurrChainId();

		// 此方法其实只会初始化一次Executor，不会每次都会初始化。Executor是唯一的
		ExecutorService parallelExecutor = ExecutorHelper.loadInstance()
				.buildWhenExecutor(this.getThreadExecutorClass());

		// 获得liteflow的参数
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

		// 定义是否中断参数
		// 这里为什么要定义成数组呢，因为后面lambda要用到，根据final不能修改引用的原则，这里用了数组对象
		final boolean[] interrupted = { false };

		// 这里主要是做了封装CompletableFuture对象，用lumbda表达式做了很多事情，这句代码要仔细理清
		// 1.先进行过滤，前置和后置组件过滤掉，因为在EL Chain处理的时候已经提出来了
		// 2.过滤isAccess为false的情况，因为不过滤这个的话，如果加上了any，那么isAccess为false那就是最快的了
		// 3.根据condition.getNodeList()的集合进行流处理，用map进行把executable对象转换成List<CompletableFuture<WhenFutureObj>>
		// 4.在转的过程中，套入CompletableFutureTimeout方法进行超时判断，如果超时则用WhenFutureObj.timeOut返回超时的对象
		// 5.第2个参数是主要的本体CompletableFuture，传入了ParallelSupplier和线程池对象
		if (ObjectUtil.isNull(this.getMaxWaitTime())) {
			if (ObjectUtil.isNotNull(liteflowConfig.getWhenMaxWaitSeconds())) {
				// 获取全局异步线程最长等待秒数
				this.setMaxWaitTime(liteflowConfig.getWhenMaxWaitSeconds());
				this.setMaxWaitTimeUnit(TimeUnit.SECONDS);
			} else {
				// 获取全局异步线程最⻓的等待时间
				this.setMaxWaitTime(liteflowConfig.getWhenMaxWaitTime());
			}
		}

		if (ObjectUtil.isNull(this.getMaxWaitTimeUnit())) {
			// 获取全局异步线程最⻓的等待时间单位
			this.setMaxWaitTimeUnit(liteflowConfig.getWhenMaxWaitTimeUnit());
		}

		List<CompletableFuture<WhenFutureObj>> completableFutureList = this.getExecutableList()
				.stream()
				.filter(executable -> !(executable instanceof PreCondition) && !(executable instanceof FinallyCondition))
				.filter(executable -> {
					try {
						return executable.isAccess(slotIndex);
					} catch (Exception e) {
						LOG.error("there was an error when executing the when component isAccess", e);
						return false;
					}
				})
				.map(executable -> CompletableFutureTimeout.completeOnTimeout(
						WhenFutureObj.timeOut(executable.getId()),
						CompletableFuture.supplyAsync(new ParallelSupplier(executable, currChainName, slotIndex),
								parallelExecutor),
						this.getMaxWaitTime(), this.getMaxWaitTimeUnit()))
				.collect(Collectors.toList());

		CompletableFuture<?> resultCompletableFuture;

		// 这里判断执行方式
		// 如果any为false，说明这些异步任务全部执行好或者超时，才返回
		// 如果any为true，说明这些异步任务只要任意一个执行完成，就返回
		if (this.isAny()) {
			// 把这些CompletableFuture通过anyOf合成一个CompletableFuture
			resultCompletableFuture = CompletableFuture
					.anyOf(completableFutureList.toArray(new CompletableFuture[] {}));
		} else {
			// 把这些CompletableFuture通过allOf合成一个CompletableFuture
			resultCompletableFuture = CompletableFuture
					.allOf(completableFutureList.toArray(new CompletableFuture[] {}));
		}

		try {
			// 进行执行，这句执行完后，就意味着所有的任务要么执行完毕，要么超时返回
			resultCompletableFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("there was an error when executing the CompletableFuture", e);
			interrupted[0] = true;
		}

		// 拿到已经完成的CompletableFuture
		// 如果any为false，那么所有任务都已经完成
		// 如果any为true，那么这里拿到的是第一个完成的任务
		// 这里过滤和转换一起用lumbda做了
		List<WhenFutureObj> allCompletableWhenFutureObjList = completableFutureList.stream().filter(f -> {
			// 过滤出已经完成的，没完成的就直接终止
			if (f.isDone()) {
				return true;
			} else {
				f.cancel(true);
				return false;
			}
		}).map(f -> {
			try {
				return f.get();
			} catch (InterruptedException | ExecutionException e) {
				interrupted[0] = true;
				return null;
			}
		}).collect(Collectors.toList());

		// 判断超时，上面已经拿到了所有已经完成的CompletableFuture
		// 那我们只要过滤出超时的CompletableFuture
		List<WhenFutureObj> timeOutWhenFutureObjList = allCompletableWhenFutureObjList.stream()
				.filter(WhenFutureObj::isTimeout)
				.collect(Collectors.toList());

		// 输出超时信息
		timeOutWhenFutureObjList.forEach(whenFutureObj -> LOG.warn(
				"executing thread has reached max-wait-seconds, thread canceled.Execute-item: [{}]", whenFutureObj.getExecutorName()));

		// 当配置了ignoreError = false，出现interrupted或者!f.get()的情况，将抛出WhenExecuteException
		if (!this.isIgnoreError()) {
			if (interrupted[0]) {
				throw new WhenExecuteException(StrUtil
						.format("requestId [{}] when execute interrupted. errorResume [false].", slot.getRequestId()));
			}

			// 循环判断CompletableFuture的返回值，如果异步执行失败，则抛出相应的业务异常
			for (WhenFutureObj whenFutureObj : allCompletableWhenFutureObjList) {
				if (!whenFutureObj.isSuccess()) {
					LOG.info(StrUtil.format("when-executor[{}] execute failed. errorResume [false].", whenFutureObj.getExecutorName()));
					throw whenFutureObj.getEx();
				}
			}
		} else if (interrupted[0]) {
			// 这里由于配置了ignoreError，所以只打印warn日志
			LOG.warn("executing when condition timeout , but ignore with errorResume.");
		}
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

	public boolean isAny() {
		return any;
	}

	public void setAny(boolean any) {
		this.any = any;
	}

	public String getThreadExecutorClass() {
		return threadExecutorClass;
	}

	public void setThreadExecutorClass(String threadExecutorClass) {
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
