package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.test.asyncNode.exception.TestException;
import org.springframework.stereotype.Component;


@Component("i")
@LiteflowCmpDefine
public class ICmp {

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS)
    public void process(NodeComponent bindCmp) throws Exception {
        Slot slot = bindCmp.getSlot();
        if (slot.hasData("count")){
            Integer count = slot.getData("count");
            slot.setData("count", ++count);
        } else{
            slot.setData("count", 1);
        }
        System.out.println("Icomp executed! throw Exception!");
        throw new TestException();
    }
}
