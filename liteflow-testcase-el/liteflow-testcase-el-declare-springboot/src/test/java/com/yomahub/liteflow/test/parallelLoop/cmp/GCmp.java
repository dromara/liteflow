package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.test.exception.CustomStatefulException;
import org.springframework.stereotype.Component;

@Component("g")
public class GCmp {

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS)
    public void process(NodeComponent bindCmp) {
        if(bindCmp.getLoopIndex()==1){
            throw new CustomStatefulException("300", "chain execute custom stateful execption");
        }
        System.out.println("GCmp executed!");
    }

}
