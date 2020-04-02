/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.spring;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.util.LOGOPrinter;

public class ComponentScaner implements BeanPostProcessor, PriorityOrdered {

	private static final Logger LOG = LoggerFactory.getLogger(ComponentScaner.class);

	public static Map<String, NodeComponent> nodeComponentMap = new HashMap<String, NodeComponent>();

	static {
		LOGOPrinter.print();
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		Class clazz = bean.getClass();
		if(NodeComponent.class.isAssignableFrom(clazz)){
			LOG.info("component[{}] has been found",beanName);
			NodeComponent nodeComponent = (NodeComponent)bean;
			nodeComponent.setNodeId(beanName);
			nodeComponentMap.put(beanName, nodeComponent);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
