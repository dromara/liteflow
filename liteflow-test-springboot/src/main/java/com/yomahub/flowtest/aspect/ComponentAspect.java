package com.yomahub.flowtest.aspect;

import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.entity.data.Slot;
import org.springframework.stereotype.Component;

@Component
public class ComponentAspect implements ICmpAroundAspect {
    @Override
    public void beforeProcess(Slot slot) {
        System.out.println("before process");
    }

    @Override
    public void afterProcess(Slot slot) {
        System.out.println("after process");
    }
}
