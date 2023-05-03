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
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.util.AnnoUtil;
import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.script.ScriptBeanManager;
import com.yomahub.liteflow.script.annotation.ScriptBean;
import com.yomahub.liteflow.script.annotation.ScriptMethod;
import com.yomahub.liteflow.script.proxy.ScriptBeanProxy;
import com.yomahub.liteflow.script.proxy.ScriptMethodProxy;
import com.yomahub.liteflow.util.LOGOPrinter;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组件扫描类，只要是NodeComponent的实现类，都可以被这个扫描器扫到
 *
 * @author Bryan.Zhang
 */
public class ComponentScanner implements BeanPostProcessor {

	/**
	 * @RefreshScope 注解 bean 的前缀
	 */
	private static final String REFRESH_SCOPE_ANN_BEAN_PREFIX = "scopedTarget.";

	/**
	 * @RefreshScope 注解完整类路径
	 */
	private static final String REFRESH_SCOPE_ANN_CLASS_PATH = "org.springframework.cloud.context.config.annotation.RefreshScope";

	private static final Logger LOG = LoggerFactory.getLogger(ComponentScanner.class);

	public static Map<String, NodeComponent> nodeComponentMap = new HashMap<>();

	private LiteflowConfig liteflowConfig;

	public static ICmpAroundAspect cmpAroundAspect;

	public ComponentScanner() {
		LOGOPrinter.print();
	}

	public ComponentScanner(LiteflowConfig liteflowConfig) {
		this.liteflowConfig = liteflowConfig;
		if (liteflowConfig.getPrintBanner()) {
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
		Class clazz = LiteFlowProxyUtil.getUserClass(bean.getClass());

		// 判断是不是声明式组件
		// 如果是，就缓存到类属性的map中
		if (LiteFlowProxyUtil.isDeclareCmp(bean.getClass())) {
			LOG.info("proxy component[{}] has been found", beanName);
			List<NodeComponent> nodeComponents = LiteFlowProxyUtil.proxy2NodeComponent(bean, beanName);
			nodeComponents.forEach(nodeComponent -> {
				String nodeId = nodeComponent.getNodeId();
				nodeId = StrUtil.isEmpty(nodeId) ? getRealBeanName(clazz, beanName) : nodeId;
				nodeComponentMap.put(nodeId, nodeComponent);
			});
			// 只有注解支持单bean多Node,所以一个直接返回
			if (nodeComponents.size() == 1) {
				return nodeComponents.get(0);
			}
			return bean;
		}

		// 组件的扫描发现，扫到之后缓存到类属性map中
		if (NodeComponent.class.isAssignableFrom(clazz)) {
			LOG.info("component[{}] has been found", beanName);
			NodeComponent nodeComponent = (NodeComponent) bean;
			nodeComponentMap.put(getRealBeanName(clazz, beanName), nodeComponent);
			return nodeComponent;
		}

		// 组件Aop的实现类加载
		if (ICmpAroundAspect.class.isAssignableFrom(clazz)) {
			LOG.info("component aspect implement[{}] has been found", beanName);
			cmpAroundAspect = (ICmpAroundAspect) bean;
			return cmpAroundAspect;
		}

		// 扫描@ScriptBean修饰的类
		ScriptBean scriptBean = AnnoUtil.getAnnotation(clazz, ScriptBean.class);
		if (ObjectUtil.isNotNull(scriptBean)) {
			ScriptBeanProxy proxy = new ScriptBeanProxy(bean, clazz, scriptBean);
			ScriptBeanManager.addScriptBean(scriptBean.value(), proxy.getProxyScriptBean());
			return bean;
		}

		// 扫描@ScriptMethod修饰的类
		List<Method> scriptMethods = Arrays.stream(clazz.getMethods()).filter(method -> {
			ScriptMethod scriptMethod = AnnoUtil.getAnnotation(method, ScriptMethod.class);
			return ObjectUtil.isNotNull(scriptMethod) && StrUtil.isNotEmpty(scriptMethod.value());
		}).collect(Collectors.toList());
		if (CollUtil.isNotEmpty(scriptMethods)) {
			Map<String, List<Method>> scriptMethodsGroupByValue = CollStreamUtil.groupBy(scriptMethods, method -> {
				ScriptMethod scriptMethod = AnnoUtil.getAnnotation(method, ScriptMethod.class);
				return scriptMethod.value();
			}, Collectors.toList());

			for (Map.Entry<String, List<Method>> entry : scriptMethodsGroupByValue.entrySet()) {
				String key = entry.getKey();
				List<Method> methods = entry.getValue();
				ScriptMethodProxy proxy = new ScriptMethodProxy(bean, clazz, methods);

				ScriptBeanManager.addScriptBean(key, proxy.getProxyScriptMethod());

			}

			return bean;
		}

		return bean;
	}

	/**
	 * 用于清除 spring 上下文扫描到的组件实体
	 */
	public static void cleanCache() {
		nodeComponentMap.clear();
	}

	/**
	 * 获取真实的 beanName 1. @RefreshScope 注解标注的bean 名称前会多加一个 scopedTarget.
	 *
	 * @param clazz    clazz
	 * @param beanName beanName
	 */
	private String getRealBeanName(Class<?> clazz, String beanName) {
		if (beanName.startsWith(REFRESH_SCOPE_ANN_BEAN_PREFIX)) {
			Annotation[] annotations = AnnotationUtil.getAnnotations(clazz, true);
			for (Annotation annotation : annotations) {
				String name = annotation.annotationType().getName();
				if (REFRESH_SCOPE_ANN_CLASS_PATH.equals(name)) {
					return beanName.replace(REFRESH_SCOPE_ANN_BEAN_PREFIX, "");
				}

			}
		}
		return beanName;
	}

}
