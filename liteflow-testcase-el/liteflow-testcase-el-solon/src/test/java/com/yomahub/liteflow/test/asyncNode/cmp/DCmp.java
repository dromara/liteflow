package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.noear.solon.annotation.Component;


@Component("d")
public class DCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        Thread.sleep(1000);
        DefaultContext context = this.getFirstContextBean();
        synchronized (NodeComponent.class){
            if (context.hasData("check")){
                String str = context.getData("check");
                str += this.getNodeId();
                context.setData("check", str);
            }else{
                context.setData("check", this.getNodeId());
            }
        }
        System.out.println("Dcomp executed!");
    }
}
