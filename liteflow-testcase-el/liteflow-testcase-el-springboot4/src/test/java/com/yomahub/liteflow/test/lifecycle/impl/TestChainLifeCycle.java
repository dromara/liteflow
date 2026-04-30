package com.yomahub.liteflow.test.lifecycle.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.lifecycle.PostProcessChainBuildLifeCycle;
import org.springframework.stereotype.Component;

@Component
public class TestChainLifeCycle implements PostProcessChainBuildLifeCycle {
    @Override
    public void postProcessBeforeChainBuild(Chain chain) {
        System.out.println(StrUtil.format("Chain Build生命周期(前)——[{}]已被加载",chain.getChainId()));
    }

    @Override
    public void postProcessAfterChainBuild(Chain chain) {
        System.out.println(StrUtil.format("Chain Build生命周期(后)——[{}]已被加载",chain.getChainId()));
    }
}
