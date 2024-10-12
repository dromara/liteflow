package com.yomahub.liteflow.test.lifecycle.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.lifecycle.PostProcessChainExecuteLifeCycle;
import com.yomahub.liteflow.slot.Slot;
import org.springframework.stereotype.Component;

@Component
public class TestChainExecuteLifeCycle implements PostProcessChainExecuteLifeCycle {
    @Override
    public void postProcessBeforeChainExecute(String chainId, Slot slot) {
        System.out.println(StrUtil.format("Chain Execute 生命周期(前)——[{}]已被加载",chainId));
    }

    @Override
    public void postProcessAfterChainExecute(String chainId, Slot slot) {
        System.out.println(StrUtil.format("Chain Execute 生命周期(后)——[{}]已被加载",chainId));
    }
}
