package com.yomahub.liteflow.test.asyncNode.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.asyncNode.exception.TestException;
import org.noear.solon.annotation.Component;


@Component("i")
public class ICmp extends NodeComponent {

    @Override
    public void process() throws Exception {
        DefaultContext context = this.getFirstContextBean();
        synchronized (ICmp.class){
            if (context.hasData("count")){
                Integer count = context.getData("count");
                context.setData("count", ++count);
            } else{
                context.setData("count", 1);
            }
        }
        System.out.println("Icomp executed! throw Exception!");
        throw new TestException();
    }
}
