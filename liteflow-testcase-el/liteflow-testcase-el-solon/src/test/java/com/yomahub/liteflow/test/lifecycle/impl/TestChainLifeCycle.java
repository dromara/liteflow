package com.yomahub.liteflow.test.lifecycle.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.lifecycle.PostProcessAfterChainBuildLifeCycle;
import org.noear.solon.annotation.Component;

@Component
public class TestChainLifeCycle implements PostProcessAfterChainBuildLifeCycle {
    @Override
    public void postProcessAfterChainBuild(Chain chain) {
        System.out.println(StrUtil.format("Chain生命周期——[{}]已被加载",chain.getChainId()));
    }
}
