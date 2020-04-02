/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.core;

import com.yomahub.liteflow.entity.flow.Executable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yomahub.liteflow.entity.flow.Node;
import com.yomahub.liteflow.entity.data.CmpStep;
import com.yomahub.liteflow.entity.data.CmpStepType;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.entity.monitor.CompStatistics;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.monitor.MonitorBus;

public abstract class NodeComponent {

	private static final Logger LOG = LoggerFactory.getLogger(NodeComponent.class);

	private InheritableThreadLocal<Integer> slotIndexTL = new InheritableThreadLocal<Integer>();

	private String nodeId;

	//是否结束整个流程，这个只对串行流程有效，并行流程无效
	private InheritableThreadLocal<Boolean> isEndTL = new InheritableThreadLocal<>();

	public void execute() throws Exception{
		Slot slot = this.getSlot();
		LOG.info("[{}]:[O]start component[{}] execution",slot.getRequestId(),this.getClass().getSimpleName());
		slot.addStep(new CmpStep(nodeId, CmpStepType.START));
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		process();

		stopWatch.stop();
		long timeSpent = stopWatch.getTime();

		slot.addStep(new CmpStep(nodeId, CmpStepType.END));

		//性能统计
		CompStatistics statistics = new CompStatistics();
		statistics.setComponentClazzName(this.getClass().getSimpleName());
		statistics.setTimeSpent(timeSpent);
		MonitorBus.load().addStatistics(statistics);


		if(this instanceof NodeCondComponent){
			String condNodeId = slot.getCondResult(this.getClass().getName());
			if(StringUtils.isNotBlank(condNodeId)){
				Node thisNode = FlowBus.getNode(nodeId);
				Executable condExecutor = thisNode.getCondNode(condNodeId);
				if(condExecutor != null){
					condExecutor.execute(slotIndexTL.get());
				}
			}
		}

		LOG.debug("[{}]:componnet[{}] finished in {} milliseconds",slot.getRequestId(),this.getClass().getSimpleName(),timeSpent);
	}

	public abstract void process() throws Exception;

	/**
	 * 是否进入该节点
	 * @return boolean
	 */
	public boolean isAccess(){
		return true;
	}

	/**
	 * 出错是否继续执行(这个只适用于串行流程，并行节点不起作用)
	 * @return boolean
	 */
	public boolean isContinueOnError() {
		return false;
	}

	/**
	 * 是否结束整个流程(不往下继续执行)
	 * @return boolean
	 */
	public boolean isEnd() {
		Boolean isEnd = isEndTL.get();
		if(isEnd == null){
			return false;
		}else{
			return isEndTL.get();
		}
	}

	/**
	 * 设置是否结束整个流程
	 * @param isEnd
	 */
	public void setIsEnd(boolean isEnd){
		this.isEndTL.set(isEnd);
	}

	public void removeIsEnd(){
		this.isEndTL.remove();
	}

	public NodeComponent setSlotIndex(Integer slotIndex) {
		this.slotIndexTL.set(slotIndex);
		return this;
	}

	public Integer getSlotIndex() {
		return this.slotIndexTL.get();
	}

	public void removeSlotIndex(){
		this.slotIndexTL.remove();
	}

	public <T extends Slot> T getSlot(){
		return DataBus.getSlot(this.slotIndexTL.get());
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
}
