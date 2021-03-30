/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.core;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.yomahub.liteflow.entity.data.CmpStep;
import com.yomahub.liteflow.entity.data.CmpStepType;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.entity.flow.Executable;
import com.yomahub.liteflow.entity.flow.Node;
import com.yomahub.liteflow.entity.monitor.CompStatistics;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.monitor.MonitorBus;
import com.yomahub.liteflow.spring.ComponentScaner;

/**
 * 普通组件抽象类
 * @author Bryan.Zhang
 */
public abstract class NodeComponent {

	private static final Logger LOG = LoggerFactory.getLogger(NodeComponent.class);

	private TransmittableThreadLocal<Integer> slotIndexTL = new TransmittableThreadLocal<>();

	@Autowired(required = false)
	private MonitorBus monitorBus;

	private String nodeId;

	//这是自己的实例，取代this
	//为何要设置这个，用this不行么，因为如果有aop去切的话，this在spring的aop里是切不到的。self对象有可能是代理过的对象
	private NodeComponent self;

	//是否结束整个流程，这个只对串行流程有效，并行流程无效
	private TransmittableThreadLocal<Boolean> isEndTL = new TransmittableThreadLocal<>();

	public void execute() throws Exception{
		Slot slot = this.getSlot();
		LOG.info("[{}]:[O]start component[{}] execution",slot.getRequestId(),this.getClass().getSimpleName());
		slot.addStep(new CmpStep(nodeId, CmpStepType.SINGLE));
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// process前置处理
		if (ObjectUtil.isNotNull(ComponentScaner.cmpAroundAspect)) {
			ComponentScaner.cmpAroundAspect.beforeProcess(this.getNodeId(), slot);
		}

		self.process();

		// process后置处理
		if (ObjectUtil.isNotNull(ComponentScaner.cmpAroundAspect)) {
			ComponentScaner.cmpAroundAspect.afterProcess(this.getNodeId(), slot);
		}

		stopWatch.stop();
		
//		slot.addStep(new CmpStep(nodeId, CmpStepType.END));
		final long timeSpent = stopWatch.getTotalTimeMillis();
		// 性能统计
		if (ObjectUtil.isNotNull(monitorBus)) {
			CompStatistics statistics = new CompStatistics(this.getClass().getSimpleName(), timeSpent);
			monitorBus.addStatistics(statistics);
		}

		if (this instanceof NodeCondComponent) {
			String condNodeId = slot.getCondResult(this.getClass().getName());
			if (StrUtil.isNotBlank(condNodeId)) {
				Node thisNode = FlowBus.getNode(nodeId);
				Executable condExecutor = thisNode.getCondNode(condNodeId);
				if (ObjectUtil.isNotNull(condExecutor)) {
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
		if(ObjectUtil.isNull(isEnd)){
			return false;
		} else {
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

	public NodeComponent getSelf() {
		return self;
	}

	public void setSelf(NodeComponent self) {
		this.self = self;
	}
}
