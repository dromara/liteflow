package com.yomahub.liteflow.test.abstractChain.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import org.springframework.stereotype.Component;

@Component("d")
@LiteflowCmpDefine(NodeTypeEnum.COMMON)
public class DCmp {
 @LiteflowMethod(LiteFlowMethodEnum.PROCESS)
 public void process(NodeComponent bindCmp) {
  System.out.println("DCmp executed!");
 }
}
