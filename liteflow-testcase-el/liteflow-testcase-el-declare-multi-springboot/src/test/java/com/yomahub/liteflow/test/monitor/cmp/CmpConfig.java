package com.yomahub.liteflow.test.monitor.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class CmpConfig {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS,nodeId = "a")
    public void processA(NodeComponent bindCmp) {
        try {
            Thread.sleep(new Random().nextInt(2000));
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("ACmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS,nodeId = "b")
    public void processB(NodeComponent bindCmp) {
        try {
            Thread.sleep(new Random().nextInt(2000));
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("BCmp executed!");
    }
    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS,nodeId = "c")
    public void process(NodeComponent bindCmp) {
        try {
            Thread.sleep(new Random().nextInt(2000));
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("BCmp executed!");
    }
}


