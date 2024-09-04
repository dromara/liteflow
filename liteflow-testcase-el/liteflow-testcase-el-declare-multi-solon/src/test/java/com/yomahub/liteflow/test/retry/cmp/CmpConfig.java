package com.yomahub.liteflow.test.retry.cmp;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ELParseException;

import java.util.Iterator;
import java.util.List;

@LiteflowComponent
public class CmpConfig {

    int flagb = 0;
    int flagc = 0;
    int flagd = 0;
    int flagf = 0;
    int flagi = 0;
    int flagn = 0;
    int flagm = 0;

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
    public void processA(NodeComponent bindCmp) {
        System.out.println("ACmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
    public void processB(NodeComponent bindCmp) {
        flagb ++;
        System.out.println("BCmp executed!");
        if(flagb < 4) throw new RuntimeException();
        else flagb = 0;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_FOR, nodeId = "c", nodeType = NodeTypeEnum.FOR)
    public int processC(NodeComponent bindCmp) {
        flagc ++;
        System.out.println("CCmp executed!");
        if(flagc < 4) throw new RuntimeException();
        else return 1;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "d", nodeType = NodeTypeEnum.SWITCH)
    public String processD(NodeComponent bindCmp) {
        flagd ++;
        System.out.println("DCmp executed!");
        if(flagd < 4) throw new RuntimeException();
        else return "a";
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BOOLEAN, nodeId = "f", nodeType = NodeTypeEnum.BOOLEAN)
    public boolean processF(NodeComponent bindCmp) {
        System.out.println("FCmp executed!");
        flagf ++;
        if(flagf < 4) throw new RuntimeException();
        else return true;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_ITERATOR, nodeId = "i", nodeType = NodeTypeEnum.ITERATOR)
    public Iterator<?> processI(NodeComponent bindCmp) {
        flagi ++;
        if(flagi < 4) throw new RuntimeException();
        else {
            List<String> list = ListUtil.toList("jack");
            return list.iterator();
        }
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "m")
    public void processM(NodeComponent bindCmp) {
        flagm ++;
        System.out.println("MCmp executed!");
        if(flagm < 4) throw new ELParseException("MCmp error!");
        else flagm = 0;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BOOLEAN, nodeId = "n", nodeType = NodeTypeEnum.BOOLEAN)
    public boolean processN(NodeComponent bindCmp) {
        flagn ++;
        System.out.println("NCmp executed!");
        if(flagn < 4) throw new RuntimeException();
        else return flagn == 4 ? true : false;
    }


}
