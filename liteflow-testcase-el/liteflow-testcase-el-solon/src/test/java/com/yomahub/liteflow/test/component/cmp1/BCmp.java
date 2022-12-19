package com.yomahub.liteflow.test.component.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;

import java.util.Objects;


@Component("b")
public class BCmp extends NodeComponent {
    @Override
    public void process() {
        System.out.println("BComp executed!");
        Integer requestData = this.getRequestData();
        Integer divisor = 130;
        Integer result = divisor / requestData;
        this.getSlot().setResponseData(result);
    }

    @Override
    public boolean isAccess() {
        Integer requestData = this.getRequestData();
        if (Objects.nonNull(requestData)){
            return true;
        }
        return false;
    }

}
