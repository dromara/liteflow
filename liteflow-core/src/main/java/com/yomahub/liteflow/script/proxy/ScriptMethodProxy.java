package com.yomahub.liteflow.script.proxy;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.exception.ScriptBeanMethodInvokeException;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;
import com.yomahub.liteflow.util.SerialsUtil;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 脚本方法代理
 */
public class ScriptMethodProxy {

	/**
	 * 被代理的 bean
	 */
	private final Object bean;

	/**
	 * 原始的类
	 */
	private final Class<?> orignalClass;

	private final List<Method> scriptMethods;

	public ScriptMethodProxy(Object bean, Class<?> orignalClass, List<Method> scriptMethods) {
		this.bean = bean;
		this.orignalClass = orignalClass;
		this.scriptMethods = scriptMethods;
	}

	public Object getProxyScriptMethod() {

		try {
			return new ByteBuddy().subclass(orignalClass)
				.name(buildClassName()) // 设置生成的类名
				.implement(bean.getClass().getInterfaces())
				.method(ElementMatchers.any())
				.intercept(InvocationHandlerAdapter.of(new AopInvocationHandler(bean, scriptMethods)))
				.annotateType(orignalClass.getAnnotations())
				.make()
				.load(ScriptBeanProxy.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
				.getLoaded()
				.newInstance();
		}
		catch (Exception e) {
			throw new LiteFlowException(e);
		}
	}

	private String buildClassName() {
		return StrUtil.format("{}.ByteBuddy${}", ClassUtil.getPackage(orignalClass), SerialsUtil.generateShortUUID());
	}

	public static class AopInvocationHandler implements InvocationHandler {

		private final Object bean;

		private final Class<?> clazz;

		private final Set<Method> methodSet;

		public AopInvocationHandler(Object bean, List<Method> methods) {
			this.bean = bean;
			this.clazz = LiteFlowProxyUtil.getUserClass(bean.getClass());
			this.methodSet = new HashSet<>(methods);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Optional<Method> invokeMethodOp = Arrays.stream(clazz.getMethods()).filter(method::equals).findFirst();

			if (!invokeMethodOp.isPresent()) {
				String errorMsg = StrUtil.format("cannot find method[{}]", clazz.getName(), method.getName());
				throw new ScriptBeanMethodInvokeException(errorMsg);
			}

			if (!methodSet.contains(method)) {
				String errorMsg = StrUtil.format("script method[{}.{}] cannot be executed", clazz.getName(),
						method.getName());
				throw new ScriptBeanMethodInvokeException(errorMsg);
			}

			return invokeMethodOp.get().invoke(bean, args);
		}

	}

}
