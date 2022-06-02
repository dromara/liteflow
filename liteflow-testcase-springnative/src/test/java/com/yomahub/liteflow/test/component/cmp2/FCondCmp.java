package com.yomahub.liteflow.test.component.cmp2;

import com.yomahub.liteflow.core.NodeCondComponent;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component("f")
public class FCondCmp extends NodeCondComponent {
    @Override
    public String processCond() {
        Integer requestData = this.getRequestData();
        if (Objects.isNull(requestData)){
            return "d";
        } else if(requestData == 0){
            return "c";
        } else {
            return "b";
        }
    }
}
