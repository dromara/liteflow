package com.yomahub.liteflow.test.component.cmp2;

import com.yomahub.liteflow.annotation.LiteflowSwitchCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component("f")
@LiteflowSwitchCmpDefine
public class FCondCmp{
    @LiteflowMethod(LiteFlowMethodEnum.PROCESS_COND)
    public String processCond(NodeComponent bindCmp) {
        Integer requestData = bindCmp.getRequestData();
        if (Objects.isNull(requestData)){
            return "d";
        } else if(requestData == 0){
            return "c";
        } else {
            return "b";
        }
    }
}
