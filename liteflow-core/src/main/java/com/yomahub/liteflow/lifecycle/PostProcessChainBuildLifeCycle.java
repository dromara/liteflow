package com.yomahub.liteflow.lifecycle;

import com.yomahub.liteflow.flow.element.Chain;

/**
 * 生命周期接口
 * 在Chain构造时期，如果有实现的话
 *
 * @author Bryan.Zhang
 * @since 2.12.4
 */
public interface PostProcessChainBuildLifeCycle extends LifeCycle {

    void postProcessBeforeChainBuild(Chain chain);

    void postProcessAfterChainBuild(Chain chain);
}
