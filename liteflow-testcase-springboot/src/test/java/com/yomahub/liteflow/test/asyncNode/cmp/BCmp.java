package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.Slot;
import org.springframework.stereotype.Component;


@Component("b")
public class BCmp extends NodeComponent {
    @Override
    public void process() {
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
        System.out.println("Bcomp executed!");
    }
}
