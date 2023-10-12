package com.yomahub.liteflow.test.abstractChain.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import org.springframework.stereotype.Component;

@Component("c")
@LiteflowCmpDefine(NodeTypeEnum.IF)
public class CCmp {
    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_IF, nodeType = NodeTypeEnum.IF)
    public boolean processIf(NodeComponent bindCmp) throws Exception {
        return true;
    }
}
