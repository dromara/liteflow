/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.data;

import java.text.MessageFormat;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 组件步骤对象
 * @author Bryan.Zhang
 */
public class CmpStep {

    private String nodeId;

    private String nodeName;

    private CmpStepType stepType;

    public CmpStep(String nodeId, String nodeName, CmpStepType stepType) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.stepType = stepType;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public CmpStepType getStepType() {
        return stepType;
    }

    public void setStepType(CmpStepType stepType) {
        this.stepType = stepType;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public String toString() {
        if (stepType.equals(CmpStepType.SINGLE)) {
        	if (StrUtil.isBlank(nodeName)){
				return StrUtil.format("{}", nodeId);
			}else{
				return StrUtil.format("{}[{}]", nodeId, nodeName);
			}
        } else {
        	if (StrUtil.isBlank(nodeName)){
				return StrUtil.format("{}({})", nodeId, stepType);
			}else{
				return StrUtil.format("{}[{}]({})", nodeId, nodeName, stepType);
			}
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (ObjectUtil.isNull(obj)) {
            return false;
        } else {
            if (getClass() != obj.getClass()) {
                return false;
            } else {
                if (((CmpStep) obj).getNodeId().equals(this.getNodeId())) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }
}
