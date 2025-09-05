package com.yomahub.liteflow.test.script.javapro.tag.aspect;

import com.yomahub.liteflow.aop.ICmpAroundAspect;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component
public class GlobalAspect implements ICmpAroundAspect {
    @Override
    public void beforeProcess(NodeComponent cmp) {
        System.out.println("组件" + cmp.getNodeId() + "，切面获取的tag："  + cmp.getTag());
    }

    @Override
    public void afterProcess(NodeComponent cmp) {

    }

    @Override
    public void onSuccess(NodeComponent cmp) {

    }

    @Override
    public void onError(NodeComponent cmp, Exception e) {

    }
}
