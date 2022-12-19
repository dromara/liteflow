package com.yomahub.liteflow.test.component.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;

import java.util.Objects;


@Component("d")
public class DCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        System.out.println("DComp executed!");
    }

    @Override
    public boolean isEnd() {
        //组件的process执行完之后才会执行isEnd
        Object requestData = this.getSlot().getResponseData();
        if (Objects.isNull(requestData)){
            System.out.println("DComp flow isEnd, because of responseData is null.");
            return true;
        }
        return false;
    }
}
