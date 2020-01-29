/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-7-28
 * @version 1.0
 */
package com.thebeastshop.liteflow.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thebeastshop.liteflow.entity.config.Node;
import com.thebeastshop.liteflow.entity.data.CmpStep;
import com.thebeastshop.liteflow.entity.data.CmpStepType;
import com.thebeastshop.liteflow.entity.data.DataBus;
import com.thebeastshop.liteflow.entity.data.Slot;
import com.thebeastshop.liteflow.entity.monitor.CompStatistics;
import com.thebeastshop.liteflow.flow.FlowBus;
import com.thebeastshop.liteflow.monitor.MonitorBus;

public abstract class NodeComponent {

	private static final Logger LOG = LoggerFactory.getLogger(NodeComponent.class);

	private InheritableThreadLocal<Integer> slotIndexTL = new InheritableThreadLocal<Integer>();

	private String nodeId;

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
				Node condNode = thisNode.getCondNode(condNodeId);
				if(condNode != null){
					NodeComponent condComponent = condNode.getInstance();
					condComponent.setSlotIndex(slotIndexTL.get());
					condComponent.execute();
				}
			}
		}

		LOG.debug("[{}]:componnet[{}] finished in {} milliseconds",slot.getRequestId(),this.getClass().getSimpleName(),timeSpent);
	}

	protected abstract void process() throws Exception;

	/**
	 * 是否进入该节点
	 */
	protected boolean isAccess(){
		return true;
	}

	/**
	 * 出错是否继续执行
	 */
	protected boolean isContinueOnError() {
		return false;
	}

	/**
	 * 是否结束整个流程(不往下继续执行)
	 */
	protected boolean isEnd() {
		Boolean isEnd = isEndTL.get();
		if(isEnd == null){
			return false;
		}else{
			return isEndTL.get();
		}
	}

	/**
	 * 设置是否结束整个流程
	 */
	protected void setIsEnd(boolean isEnd){
		this.isEndTL.set(isEnd);
	}

	protected void removeIsEnd(){
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
