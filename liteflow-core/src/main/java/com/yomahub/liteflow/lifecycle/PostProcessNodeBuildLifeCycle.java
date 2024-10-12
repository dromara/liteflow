package com.yomahub.liteflow.lifecycle;

import com.yomahub.liteflow.flow.element.Node;

/**
 * 生命周期接口
 * 在Node构造时期，如果有实现的话
 *
 * @author Bryan.Zhang
 * @since 2.12.4
 */
public interface PostProcessNodeBuildLifeCycle extends LifeCycle {

    void postProcessBeforeNodeBuild(Node node);

    void postProcessAfterNodeBuild(Node node);
}
