package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.Slot;
import org.springframework.stereotype.Component;


@Component("g")
public class GCmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        Thread.sleep(500);
        Slot slot = this.getSlot();
        synchronized (NodeComponent.class){
            if (slot.hasData("check")){
                String str = slot.getData("check");
                str += this.getNodeId();
                slot.setData("check", str);
            }else{
                slot.setData("check", this.getNodeId());
            }
        }
        System.out.println("Gcomp executed!");
    }
}
