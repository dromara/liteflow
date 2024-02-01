/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.element.condition;

import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.ChainEndException;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 串行器
 *
 * @author Bryan.Zhang
 */
public class ThenCondition extends Condition {

	@Override
	public ConditionTypeEnum getConditionType() {
		return ConditionTypeEnum.TYPE_THEN;
	}

	@Override
	public void executeCondition(Integer slotIndex) throws Exception {
		List<PreCondition> preConditionList = this.getPreConditionList();
		List<FinallyCondition> finallyConditionList = this.getFinallyConditionList();

		try {
			for (PreCondition preCondition : preConditionList) {
				preCondition.setCurrChainId(this.getCurrChainId());
				preCondition.execute(slotIndex);
			}

			for (Executable executableItem : this.getExecutableList()) {
				executableItem.setCurrChainId(this.getCurrChainId());
				executableItem.execute(slotIndex);
			}
		}
		catch (ChainEndException e) {
			// 这里单独catch ChainEndException是因为ChainEndException是用户自己setIsEnd抛出的异常
			// 是属于正常逻辑，所以会在FlowExecutor中判断。这里不作为异常处理
			throw e;
		}
		catch (Exception e) {
			Slot slot = DataBus.getSlot(slotIndex);
			//正常情况下slot不可能为null
			//当设置了超时后，还在运行的组件就有可能因为主流程已经结束释放slot而导致slot为null
			if (slot != null){
				String chainId = this.getCurrChainId();
				// 这里事先取到exception set到slot里，为了方便finally取到exception
				if (slot.isSubChain(chainId)) {
					slot.setSubException(chainId, e);
				}
				else {
					slot.setException(e);
				}
			}
			throw e;
		}
		finally {
			for (FinallyCondition finallyCondition : finallyConditionList) {
				finallyCondition.setCurrChainId(this.getCurrChainId());
				finallyCondition.execute(slotIndex);
			}
		}
	}

	@Override
	public void addExecutable(Executable executable) {
		if (executable instanceof PreCondition) {
			this.addPreCondition((PreCondition) executable);
		}
		else if (executable instanceof FinallyCondition) {
			this.addFinallyCondition((FinallyCondition) executable);
		}
		else {
			super.addExecutable(executable);
		}
	}

	public List<PreCondition> getPreConditionList() {
		return this.getExecutableList(ConditionKey.PRE_KEY)
			.stream()
			.map(executable -> (PreCondition) executable)
			.collect(Collectors.toList());
	}

	public void addPreCondition(PreCondition preCondition) {
		this.addExecutable(ConditionKey.PRE_KEY, preCondition);
	}

	public List<FinallyCondition> getFinallyConditionList() {
		return this.getExecutableList(ConditionKey.FINALLY_KEY)
			.stream()
			.map(executable -> (FinallyCondition) executable)
			.collect(Collectors.toList());
	}

	public void addFinallyCondition(FinallyCondition finallyCondition) {
		this.addExecutable(ConditionKey.FINALLY_KEY, finallyCondition);
	}

}
