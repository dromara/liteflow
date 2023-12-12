package com.yomahub.liteflow.core.proxy;

import com.yomahub.liteflow.enums.NodeTypeEnum;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 声明式组件BeanDefinition的包装类
 * @author Bryan.Zhang
 * @since 2.11.4
 */
public class DeclWarpBean {

    private String nodeId;

    private String nodeName;

    private NodeTypeEnum nodeType;

    private Object rawBean;

    private Class<?> rawClazz;

    private List<MethodWrapBean> methodWrapBeanList;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Object getRawBean() {
        return rawBean;
    }

    public void setRawBean(Object rawBean) {
        this.rawBean = rawBean;
    }

    public List<MethodWrapBean> getMethodWrapBeanList() {
        return methodWrapBeanList;
    }

    public void setMethodWrapBeanList(List<MethodWrapBean> methodWrapBeanList) {
        this.methodWrapBeanList = methodWrapBeanList;
    }

    public Class<?> getRawClazz() {
        return rawClazz;
    }

    public void setRawClazz(Class<?> rawClazz) {
        this.rawClazz = rawClazz;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public NodeTypeEnum getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeTypeEnum nodeType) {
        this.nodeType = nodeType;
    }
}
