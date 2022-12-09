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
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.slot.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * 串行器
 * @author Bryan.Zhang
 */
public class ThenCondition extends Condition {

	/**
	 * 前置处理Condition
	 */
	private final List<PreCondition> preConditionList = new ArrayList<>();

	/**
	 * 后置处理Condition
	 */
	private final List<FinallyCondition> finallyConditionList = new ArrayList<>();

	@Override
	public ConditionTypeEnum getConditionType() {
		return ConditionTypeEnum.TYPE_THEN;
	}

	@Override
	public void execute(Integer slotIndex) throws Exception {
		try{
			for (PreCondition preCondition : preConditionList){
				preCondition.execute(slotIndex);
			}

			for (Executable executableItem : this.getExecutableList()) {
				executableItem.setCurrChainId(this.getCurrChainId());
				executableItem.execute(slotIndex);
			}
		}catch (ChainEndException e){
			//这里单独catch ChainEndException是因为ChainEndException是用户自己setIsEnd抛出的异常
			//是属于正常逻辑，所以会在FlowExecutor中判断。这里不作为异常处理
			throw e;
		}catch (Exception e){
			Slot slot = DataBus.getSlot(slotIndex);
			String chainId = this.getCurrChainId();
			//这里事先取到exception set到slot里，为了方便finally取到exception
			if (slot.isSubChain(chainId)){
				slot.setSubException(chainId, e);
			}else{
				slot.setException(e);
			}
			throw e;
		}finally {
			for (FinallyCondition finallyCondition : finallyConditionList){
				finallyCondition.execute(slotIndex);
			}
		}
	}

	@Override
	public void addExecutable(Executable executable) {
		if (executable instanceof PreCondition){
			preConditionList.add((PreCondition) executable);
		}else if (executable instanceof FinallyCondition){
			finallyConditionList.add((FinallyCondition) executable);
		}else{
			super.addExecutable(executable);
		}
	}
}
