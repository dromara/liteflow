package com.yomahub.liteflow.test.component.cmp1;

import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;

import java.util.Objects;


@Component("c")
public class CCmp extends NodeComponent {
    @Override
    public void process() {
        System.out.println("CComp executed!");
        Integer requestData = this.getRequestData();
        Integer divisor = 130;
        Integer result = divisor / requestData;
        this.getSlot().setResponseData(result);
        System.out.println("responseData="+Integer.parseInt(this.getSlot().getResponseData()));
    }

    @Override
    public boolean isContinueOnError() {
        Integer requestData = this.getRequestData();
        if (Objects.nonNull(requestData)){
            return true;
        }
        return false;
    }
}
