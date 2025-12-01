package com.yomahub.liteflow.benchmark.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;

import java.util.Random;

@LiteflowComponent(id = "if_1", name = "业务判断1")
public class IF1SwitchCmp extends NodeSwitchComponent {
    @Override
    public String processSwitch() throws Exception {
        //这里写死跳到并行获取剩余量那条分支，你可以改成其他分支测试
        return "branch1";
    }
}
