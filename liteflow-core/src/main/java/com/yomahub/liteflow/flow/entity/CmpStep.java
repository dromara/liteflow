/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.entity;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.CmpStepTypeEnum;
import com.yomahub.liteflow.flow.element.Node;

import java.util.Date;

/**
 * 组件步骤对象
 *
 * @author Bryan.Zhang
 * @author Jay li
 */
public class CmpStep {

	private String nodeInstanceId;

	private String nodeId;

	private String nodeName;

	private String tag;

	private CmpStepTypeEnum stepType;

	private Date startTime;

	private Date endTime;

	// 消耗的时间，毫秒为单位
	private Long timeSpent;

	// 是否成功
	private boolean success;

	// 有exception，success一定为false
	// 但是success为false，不一定有exception，因为有可能没执行到，或者没执行结束(any)
	private Exception exception;

	private NodeComponent instance;

	// 回滚消耗的时间
	private Long rollbackTimeSpent;

	// 当前执行的node
	private Node refNode;

	// 自定义步骤数据
	private Object stepData;

	// 运行线程名称
	private String threadName;


	public CmpStep(String nodeId, String nodeName, CmpStepTypeEnum stepType) {
		this.nodeId = nodeId;
		this.nodeName = nodeName;
		this.stepType = stepType;
	}

	public String getNodeInstanceId() {
		return nodeInstanceId;
	}

	public void setNodeInstanceId(String nodeInstanceId) {
		this.nodeInstanceId = nodeInstanceId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public CmpStepTypeEnum getStepType() {
		return stepType;
	}

	public void setStepType(CmpStepTypeEnum stepType) {
		this.stepType = stepType;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public Long getTimeSpent() {
		return timeSpent;
	}

	public void setTimeSpent(Long timeSpent) {
		this.timeSpent = timeSpent;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public NodeComponent getInstance() {
		return instance;
	}

	public void setInstance(NodeComponent instance) {
		this.instance = instance;
	}

	public Long getRollbackTimeSpent() {
		return rollbackTimeSpent;
	}

	public void setRollbackTimeSpent(Long rollbackTimeSpent) {
		this.rollbackTimeSpent = rollbackTimeSpent;
	}

	public Node getRefNode() {
		return refNode;
	}

	public void setRefNode(Node refNode) {
		this.nodeInstanceId = refNode.getNodeInstanceId();
		this.refNode = refNode;
	}

	public String buildString() {
		if (stepType.equals(CmpStepTypeEnum.SINGLE)) {
			if (StrUtil.isBlank(nodeName)) {
				return StrUtil.format("{}", nodeId);
			}
			else {
				return StrUtil.format("{}[{}]", nodeId, nodeName);
			}
		}
		else {
			// 目前没有其他的类型
			return null;
		}
	}

	public String buildStringWithInstanceId() {
		if (stepType.equals(CmpStepTypeEnum.SINGLE)) {
			return StrUtil.format("{}[{}]", nodeId, nodeInstanceId);
		}
		else {
			// 目前没有其他的类型
			return null;
		}
	}

	public String buildStringWithTime() {
		if (stepType.equals(CmpStepTypeEnum.SINGLE)) {
			if (StrUtil.isBlank(nodeName)) {
				if (timeSpent != null) {
					return StrUtil.format("{}<{}>", nodeId, timeSpent);
				}
				else {
					return StrUtil.format("{}", nodeId);
				}
			}
			else {
				if (timeSpent != null) {
					return StrUtil.format("{}[{}]<{}>", nodeId, nodeName, timeSpent);
				}
				else {
					return StrUtil.format("{}[{}]", nodeId, nodeName);
				}
			}
		}
		else {
			// 目前没有其他的类型
			return null;
		}
	}

	public String buildRollbackStringWithTime() {
		if (stepType.equals(CmpStepTypeEnum.SINGLE)) {
			if (StrUtil.isBlank(nodeName)) {
				if (rollbackTimeSpent != null) {
					return StrUtil.format("{}<{}>", nodeId, rollbackTimeSpent);
				}
				else {
					return StrUtil.format("{}", nodeId);
				}
			}
			else {
				if (rollbackTimeSpent != null) {
					return StrUtil.format("{}[{}]<{}>", nodeId, nodeName, rollbackTimeSpent);
				}
				else {
					return StrUtil.format("{}[{}]", nodeId, nodeName);
				}
			}
		}
		else {
			// 目前没有其他的类型
			return null;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (ObjectUtil.isNull(obj)) {
			return false;
		}
		else {
			if (getClass() != obj.getClass()) {
				return false;
			}
			else {
				if (((CmpStep) obj).getNodeId().equals(this.getNodeId())) {
					return true;
				}
				else {
					return false;
				}
			}
		}
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Object getStepData() {
		return stepData;
	}

	public void setStepData(Object stepData) {
		this.stepData = stepData;
	}

	public String getThreadName() {
		return threadName;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}
}
