package com.yomahub.liteflow.springboot;

import com.yomahub.liteflow.core.FlowExecutor;
import org.springframework.beans.factory.InitializingBean;
import javax.annotation.Resource;

public class LiteflowExecutorInit implements InitializingBean {

    private FlowExecutor flowExecutor;

    public LiteflowExecutorInit(FlowExecutor flowExecutor) {
        this.flowExecutor = flowExecutor;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        flowExecutor.init();
    }
}
