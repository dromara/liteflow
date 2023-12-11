package com.yomahub.liteflow.core.proxy;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.exception.ComponentProxyErrorException;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;

import java.util.Arrays;

/**
 * 组件代理类通用方法 主要用于声明式组件
 *
 * @author Bryan.Zhang
 * @since 2.6.14
 */
public class LiteFlowProxyUtil {

	private static final LFLog LOG = LFLoggerManager.getLogger(LiteFlowProxyUtil.class);

	/**
	 * 判断一个bean是否是声明式组件
	 */
	public static boolean isDeclareCmp(Class<?> clazz) {
		// 查看bean里的method是否有方法标记了@LiteflowMethod标注
		// 这里的bean有可能是cglib加强过的class，所以要先进行个判断
		Class<?> targetClass = getUserClass(clazz);
		// 判断是否有方法标记了@LiteflowMethod标注，有则为声明式组件
		return Arrays.stream(targetClass.getMethods())
			.anyMatch(method -> method.getAnnotation(LiteflowMethod.class) != null);
	}

	/**
	 * 对一个满足声明式的bean进行代理,生成代理类
	 */
	public static NodeComponent proxy2NodeComponent(DeclWarpBean declWarpBean) {
		try {
			DeclComponentProxy proxy = new DeclComponentProxy(declWarpBean);
			return proxy.getProxy();
		}catch (LiteFlowException liteFlowException) {
			throw liteFlowException;
		}catch (Exception e) {
			String errMsg = StrUtil.format("Error while proxying bean[{}]", declWarpBean.getRawClazz().getName());
			LOG.error(errMsg);
			throw new ComponentProxyErrorException(errMsg);
		}
	}

	public static boolean isCglibProxyClass(Class<?> clazz) {
		return (clazz != null && isCglibProxyClassName(clazz.getName()));
	}

	public static Class<?> getUserClass(Class<?> clazz) {
		if (clazz.getName().contains("$$")) {
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null && superclass != Object.class) {
				return superclass;
			}
		}
		return clazz;
	}

	private static boolean isCglibProxyClassName(String className) {
		return (className != null && className.contains("$$"));
	}

}
