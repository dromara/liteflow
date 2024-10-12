package com.yomahub.liteflow.test.lifecycle.impl;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.lifecycle.PostProcessNodeBuildLifeCycle;
import org.noear.solon.annotation.Component;

@Component
public class TestNodeLifeCycle implements PostProcessNodeBuildLifeCycle {
    @Override
    public void postProcessBeforeNodeBuild(Node node) {
        System.out.println(StrUtil.format("Node生命周期(前)——[{}]已被加载",node.getId()));
    }

    @Override
    public void postProcessAfterNodeBuild(Node node) {
        System.out.println(StrUtil.format("Node生命周期(后)——[{}]已被加载",node.getId()));
    }
}
