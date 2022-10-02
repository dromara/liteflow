/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.spring;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.script.ScriptBean;
import com.yomahub.liteflow.script.ScriptBeanManager;
import com.yomahub.liteflow.util.LOGOPrinter;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组件扫描类，只要是NodeComponent的实现类，都可以被这个扫描器扫到
 * @author Bryan.Zhang
 */
public class ComponentScanner implements BeanPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentScanner.class);

    public static Map<String, NodeComponent> nodeComponentMap = new HashMap<>();

    private LiteflowConfig liteflowConfig;

    public static ICmpAroundAspect cmpAroundAspect;

    public ComponentScanner() {
        LOGOPrinter.print();
    }

    public ComponentScanner(LiteflowConfig liteflowConfig) {
        this.liteflowConfig = liteflowConfig;
        if(liteflowConfig.getPrintBanner()){
            // 打印liteflow的LOGO
            LOGOPrinter.print();
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = bean.getClass();

        //判断是不是声明式组件
        //如果是，就缓存到类属性的map中
        if (LiteFlowProxyUtil.isDeclareCmp(bean.getClass())){
            LOG.info("proxy component[{}] has been found", beanName);
            List<NodeComponent> nodeComponents = LiteFlowProxyUtil.proxy2NodeComponent(bean, beanName);
            nodeComponents.forEach(
                    nodeComponent -> {
                        String nodeId = nodeComponent.getNodeId();
                        nodeId = StrUtil.isEmpty(nodeId) ? beanName : nodeId;
                        nodeComponentMap.put(nodeId, nodeComponent);
                    }
            );
            // 只有注解支持单bean多Node,所以一个直接返回
            if (nodeComponents.size() == 1){
                return nodeComponents.get(0);
            }
            return bean;
        }

        // 组件的扫描发现，扫到之后缓存到类属性map中
        if (NodeComponent.class.isAssignableFrom(clazz)) {
            LOG.info("component[{}] has been found", beanName);
            NodeComponent nodeComponent = (NodeComponent) bean;
            nodeComponentMap.put(beanName, nodeComponent);
            return nodeComponent;
        }

        // 组件Aop的实现类加载
        if (ICmpAroundAspect.class.isAssignableFrom(clazz)) {
            LOG.info("component aspect implement[{}] has been found", beanName);
            cmpAroundAspect = (ICmpAroundAspect) bean;
            return cmpAroundAspect;
        }

        //扫描@ScriptBean修饰的类
        ScriptBean scriptBean = AnnotationUtil.getAnnotation(bean.getClass(), ScriptBean.class);
        if (ObjectUtil.isNotNull(scriptBean)){
            ScriptBeanManager.addScriptBean(scriptBean.value(), bean);
        }

        return bean;
    }

    /**
     * 用于清除 spring 上下文扫描到的组件实体
     */
    public static void cleanCache() {
        nodeComponentMap.clear();
    }
}

