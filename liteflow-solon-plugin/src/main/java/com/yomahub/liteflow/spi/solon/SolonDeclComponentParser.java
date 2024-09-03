package com.yomahub.liteflow.spi.solon;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.*;
import com.yomahub.liteflow.annotation.util.AnnoUtil;
import com.yomahub.liteflow.core.proxy.DeclWarpBean;
import com.yomahub.liteflow.core.proxy.MethodWrapBean;
import com.yomahub.liteflow.core.proxy.ParameterWrapBean;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.CmpDefinitionException;
import com.yomahub.liteflow.exception.NotSupportDeclException;
import com.yomahub.liteflow.spi.DeclComponentParser;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.BeanWrap;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Solon 环境声明式组件解析器实现（在 solon 里没有用上；机制不同）
 *
 * @author noear
 * */
public class SolonDeclComponentParser implements DeclComponentParser {
    @Override
    public List<DeclWarpBean> parseDeclBean(Class<?> clazz) {
        return parseDeclBean(clazz, null, null);
    }

    @Override
    public List<DeclWarpBean> parseDeclBean(Class<?> clazz, final String nodeId, final String nodeName) {
        Map<String, List<DeclInfo>> definitionMap = Arrays.stream(clazz.getMethods()).filter(
                method -> AnnotationUtil.getAnnotation(method, LiteflowMethod.class) != null
        ).map(method -> {
            LiteflowMethod liteflowMethod = AnnotationUtil.getAnnotation(method, LiteflowMethod.class);
            LiteflowRetry liteflowRetry = AnnotationUtil.getAnnotation(method, LiteflowRetry.class);

            String currNodeId = null;
            String currNodeName = null;
            if (nodeId == null){
                if (StrUtil.isBlank(liteflowMethod.nodeId())){
                    LiteflowComponent liteflowComponent = AnnoUtil.getAnnotation(clazz, LiteflowComponent.class);
                    Component component = AnnoUtil.getAnnotation(clazz, Component.class);

                    if(liteflowComponent != null){
                        currNodeId = liteflowComponent.value();
                        currNodeName = liteflowComponent.name();
                    }else if(component != null){
                        currNodeId = component.value();
                    }else{
                        currNodeName = StrUtil.EMPTY;
                        currNodeId = StrUtil.EMPTY;
                    }
                }else{
                    currNodeId = liteflowMethod.nodeId();
                    currNodeName = liteflowMethod.nodeName();
                }
            }else{
                currNodeId = nodeId;
                currNodeName = nodeName;
            }


            NodeTypeEnum nodeType;
            LiteflowCmpDefine liteflowCmpDefine = AnnotationUtil.getAnnotation(method.getDeclaringClass(), LiteflowCmpDefine.class);
            if (liteflowCmpDefine != null){
                nodeType = liteflowCmpDefine.value();
            }else{
                nodeType = liteflowMethod.nodeType();
            }


            Parameter[] parameters = method.getParameters();
            List<ParameterWrapBean> parameterList = IntStream.range(0, parameters.length).boxed().map(index -> {
                Parameter parameter = parameters[index];
                return new ParameterWrapBean(parameter.getType(), AnnotationUtil.getAnnotation(parameter, LiteflowFact.class), index);
            }).collect(Collectors.toList());



            return new DeclInfo(currNodeId, currNodeName, nodeType, method.getDeclaringClass(), new MethodWrapBean(method, liteflowMethod, liteflowRetry, parameterList));
        }).filter(declInfo -> StrUtil.isNotBlank(declInfo.getNodeId())).collect(Collectors.groupingBy(DeclInfo::getNodeId));

        return definitionMap.entrySet().stream().map(entry -> {
            String key = entry.getKey();
            List<DeclInfo> declInfos = entry.getValue();
            DeclWarpBean declWarpBean = new DeclWarpBean();
            declWarpBean.setNodeId(key);

            DeclInfo processMethodDeclInfo = declInfos.stream().filter(declInfo -> declInfo.getMethodWrapBean().getLiteflowMethod().value().isMainMethod()).findFirst().orElse(null);
            if (processMethodDeclInfo == null){
                throw new CmpDefinitionException(StrUtil.format("Component [{}] does not define the process method", key));
            }

            declWarpBean.setNodeName(processMethodDeclInfo.getNodeName());
            declWarpBean.setRawClazz(processMethodDeclInfo.getRawClazz());
            declWarpBean.setNodeType(processMethodDeclInfo.getNodeType());

            Object rawClassDefinition = Solon.context().getBeanOrNew(clazz);

            declWarpBean.setRawBean(rawClassDefinition);
            declWarpBean.setMethodWrapBeanList(declInfos.stream().map(DeclInfo::getMethodWrapBean).collect(Collectors.toList()));
            return declWarpBean;
        }).collect(Collectors.toList());
    }

    @Override
    public int priority() {
        return 1;
    }

    public static class DeclInfo{
        private String nodeId;

        private String nodeName;

        private NodeTypeEnum nodeType;

        private Class<?> rawClazz;

        private MethodWrapBean methodWrapBean;

        public DeclInfo(String nodeId, String nodeName, NodeTypeEnum nodeType, Class<?> rawClazz, MethodWrapBean methodWrapBean) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.nodeType = nodeType;
            this.rawClazz = rawClazz;
            this.methodWrapBean = methodWrapBean;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
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

        public MethodWrapBean getMethodWrapBean() {
            return methodWrapBean;
        }

        public void setMethodWrapBean(MethodWrapBean methodWrapBean) {
            this.methodWrapBean = methodWrapBean;
        }

        public NodeTypeEnum getNodeType() {
            return nodeType;
        }

        public void setNodeType(NodeTypeEnum nodeType) {
            this.nodeType = nodeType;
        }
    }
}
