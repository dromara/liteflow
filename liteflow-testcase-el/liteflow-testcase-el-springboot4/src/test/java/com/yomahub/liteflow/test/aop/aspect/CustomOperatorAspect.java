package com.yomahub.liteflow.test.aop.aspect;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class CustomOperatorAspect {

	@Pointcut("execution(* com.yomahub.liteflow.test.aop.*.*.process())")
	public void cut() {
	}

	@Around("cut()")
	public Object around(ProceedingJoinPoint jp) throws Throwable {
		NodeComponent cmp = (NodeComponent) jp.getThis();
		DefaultContext context = cmp.getFirstContextBean();
		cmp.setIsContinueOnError(true);
		context.setData(cmp.getNodeId(), "before");
		Object returnObj = jp.proceed();
		context.setData(cmp.getNodeId(), StrUtil.format("{}_{}", context.getData(cmp.getNodeId()), "after"));
		return returnObj;
	}

}
