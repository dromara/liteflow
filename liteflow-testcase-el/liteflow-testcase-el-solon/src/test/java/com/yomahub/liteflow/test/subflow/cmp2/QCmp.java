package com.yomahub.liteflow.test.subflow.cmp2;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.noear.solon.annotation.Component;

import java.util.HashSet;
import java.util.Set;

import static com.yomahub.liteflow.test.subflow.ImplicitSubFlowELSpringbootTest.RUN_TIME_SLOT;


@Component("q")
public class QCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        String requestData = this.getSubChainReqDataInAsync();
        DefaultContext context = this.getFirstContextBean();

        synchronized (QCmp.class){
            if (context.hasData("test")){
                Set<String> set = context.getData("test");
                set.add(requestData);
            }else{
                Set<String> set = new HashSet<>();
                set.add(requestData);
                context.setData("test", set);
            }
        }
    }
}
