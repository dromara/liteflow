package com.yomahub.liteflow.test.aop.aspect;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.Slot;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class CustomAspect {

    @Pointcut("execution(* com.yomahub.liteflow.test.aop.cmp1.*.process(*))")
    public void cut() {
    }

    @Around("cut()")
    public Object around(ProceedingJoinPoint jp) throws Throwable {
        NodeComponent cmp = (NodeComponent) jp.getThis();
        Slot slot = cmp.getSlot();
        slot.setData(cmp.getNodeId(), "before");
        Object returnObj = jp.proceed();
        slot.setData(cmp.getNodeId(), StrUtil.format("{}_{}", slot.getData(cmp.getNodeId()), "after"));
        return returnObj;
    }
}
