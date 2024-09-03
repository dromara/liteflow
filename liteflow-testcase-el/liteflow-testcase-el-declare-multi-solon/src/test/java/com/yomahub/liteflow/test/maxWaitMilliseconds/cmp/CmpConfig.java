package com.yomahub.liteflow.test.maxWaitMilliseconds.cmp;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.slot.DefaultContext;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@LiteflowComponent
public class CmpConfig {

    public static final String CONTENT_KEY = "testKey";

    private int count = 0;

    // 执行过的 chain
    Set<String> executedChain = new HashSet<>();

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
    public void processA(NodeComponent bindCmp) {
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("ACmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
    public void processB(NodeComponent bindCmp) {
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("BCmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
    public void process(NodeComponent bindCmp) {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("CCmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
    public void processD(NodeComponent bindCmp) {
        try {
            Thread.sleep(500);
            DefaultContext contextBean = bindCmp.getFirstContextBean();
            contextBean.setData(CONTENT_KEY, "value");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("DCmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BOOLEAN, nodeId = "f", nodeType = NodeTypeEnum.BOOLEAN)
    public boolean processIf(NodeComponent bindCmp) throws Exception {
        return true;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "s", nodeType = NodeTypeEnum.SWITCH)
    public String processSwitch(NodeComponent bindCmp) throws Exception {
        return "b";
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BOOLEAN, nodeId = "w", nodeType = NodeTypeEnum.BOOLEAN)
    public boolean processWhile(NodeComponent bindCmp) throws Exception {
        // 判断是否切换了 chain
        if (!executedChain.contains(bindCmp.getCurrChainId())) {
            count = 0;
            executedChain.add(bindCmp.getCurrChainId());
        }
        count++;
        return count <= 2;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_ITERATOR, nodeId = "x", nodeType = NodeTypeEnum.ITERATOR)
    public Iterator<?> processIterator(NodeComponent bindCmp) throws Exception {
        List<String> list = ListUtil.toList("one", "two");
        return list.iterator();
    }
}
