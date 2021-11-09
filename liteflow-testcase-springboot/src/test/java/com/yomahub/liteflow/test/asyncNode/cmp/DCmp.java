package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.Slot;
import org.springframework.stereotype.Component;


@Component("d")
public class DCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        Thread.sleep(1000);
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
        System.out.println("Dcomp executed!");
    }
}
