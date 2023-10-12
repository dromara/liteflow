package com.yomahub.liteflow.test.abstractChain.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import org.springframework.stereotype.Component;

@Component("j")
@LiteflowCmpDefine(NodeTypeEnum.COMMON)
public class JCmp {
 @LiteflowMethod(LiteFlowMethodEnum.PROCESS)
 public void process(NodeComponent bindCmp) {
  System.out.println("JCmp executed!");
 }
}
