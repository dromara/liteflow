package com.yomahub.liteflow.test.lifecycle.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.lifecycle.PostProcessFlowExecuteLifeCycle;
import com.yomahub.liteflow.slot.Slot;
import org.springframework.stereotype.Component;

@Component
public class TestFlowExecuteLifeCycle implements PostProcessFlowExecuteLifeCycle {
    @Override
    public void postProcessBeforeFlowExecute(String chainId, Slot slot) {
        System.out.println(StrUtil.format("FlowExecutor 生命周期(前)——[{}]已被加载",chainId));
    }

    @Override
    public void postProcessAfterFlowExecute(String chainId, Slot slot) {
        System.out.println(StrUtil.format("FlowExecutor 生命周期(后)——[{}]已被加载",chainId));
    }
}
