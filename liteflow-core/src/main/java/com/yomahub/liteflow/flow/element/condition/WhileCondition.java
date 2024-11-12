package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.parallel.LoopFutureObj;
import com.yomahub.liteflow.thread.ExecutorHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 循环条件Condition
 *
 * @author Bryan.Zhang
 * @author jason
 * @since 2.9.0
 */
public class WhileCondition extends LoopCondition {

	@Override
	public void executeCondition(Integer slotIndex) throws Exception {
		Executable whileItem = this.getWhileItem();

		// 提前设置 chainId，避免无法在 isAccess 方法中获取到
		whileItem.setCurrChainId(this.getCurrChainId());

		// 先去判断isAccess方法，如果isAccess方法都返回false，整个WHILE表达式不执行
		if (!whileItem.isAccess(slotIndex)) {
			return;
		}

		// 获得要循环的可执行对象
		Executable executableItem = this.getDoExecutor();

		// 获取Break节点
		Executable breakItem = this.getBreakItem();

		// 循环执行
		int index = 0;
		if(!this.isParallel()){
			//串行循环
			while (getWhileResult(slotIndex, index)) {
				executableItem.setCurrChainId(this.getCurrChainId());
				setLoopIndex(executableItem, index);
				executableItem.execute(slotIndex);
				// 如果break组件不为空，则去执行
				if (ObjectUtil.isNotNull(breakItem)) {
					breakItem.setCurrChainId(this.getCurrChainId());
					setLoopIndex(breakItem, index);
					breakItem.execute(slotIndex);
					boolean isBreak = breakItem.getItemResultMetaValue(slotIndex);
					if (isBreak) {
						break;
					}
				}
				index++;
			}
		}else{
			//并行循环逻辑
			List<CompletableFuture<LoopFutureObj>> futureList = new ArrayList<>();
			//获取并行循环的线程池
			ExecutorService parallelExecutor = ExecutorHelper.loadInstance().buildExecutorService(this, slotIndex,
																								  this.getConditionType());
			while (getWhileResult(slotIndex, index)){
				CompletableFuture<LoopFutureObj> future =
						CompletableFuture.supplyAsync(new LoopParallelSupplier(executableItem, this.getCurrChainId(), slotIndex, index), parallelExecutor);
				futureList.add(future);
				//break判断
				if (ObjectUtil.isNotNull(breakItem)) {
					breakItem.setCurrChainId(this.getCurrChainId());
					setLoopIndex(breakItem, index);
					breakItem.execute(slotIndex);
					boolean isBreak = breakItem.getItemResultMetaValue(slotIndex);
					if (isBreak) {
						break;
					}
				}
				index++;
			}
			//等待所有的异步执行完毕
			handleFutureList(futureList);
		}
	}

	private boolean getWhileResult(Integer slotIndex, int loopIndex) throws Exception {
		Executable whileItem = this.getWhileItem();
		// 执行while组件
		setLoopIndex(whileItem, loopIndex);
		whileItem.execute(slotIndex);

		return whileItem.getItemResultMetaValue(slotIndex);
	}

	@Override
	public ConditionTypeEnum getConditionType() {
		return ConditionTypeEnum.TYPE_WHILE;
	}

	public Executable getWhileItem() {
		return this.getExecutableOne(ConditionKey.WHILE_KEY);
	}

	public void setWhileItem(Executable whileItem) {
		this.addExecutable(ConditionKey.WHILE_KEY, whileItem);
	}

}
