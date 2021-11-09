package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.NodeCondComponent;
import com.yomahub.liteflow.entity.data.Slot;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;


@Component("h")
public class HCmp extends NodeComponent {

    @Override
    public void process() throws Exception {
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

        System.out.println("Hcomp executed!");
    }
}
