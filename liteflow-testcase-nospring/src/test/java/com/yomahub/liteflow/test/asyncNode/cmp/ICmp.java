package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.test.asyncNode.exception.TestException;


public class ICmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        Slot slot = this.getSlot();
        if (slot.hasData("count")) {
            Integer count = slot.getData("count");
            slot.setData("count", ++count);
        } else {
            slot.setData("count", 1);
        }
        System.out.println("Icomp executed! throw Exception!");
        throw new TestException();
    }
}