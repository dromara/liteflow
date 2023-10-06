package com.yomahub.liteflow.test.rollback.cmp;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.slot.DefaultContext;

import java.util.Iterator;
import java.util.List;

@LiteflowComponent
public class CmpConfig {

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
    public void processA(NodeComponent bindCmp) {
        System.out.println("ACmp executed!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "a")
    public void rollbackA(NodeComponent bindCmp) throws Exception {
        String testKey = "test";

        DefaultContext context = bindCmp.getFirstContextBean();
        if (context.getData(testKey) == null) {
            context.setData(testKey, bindCmp.getTag());
        }
        else {
            String s = context.getData(testKey);
            s += bindCmp.getTag();
            context.setData(testKey, s);
        }
        System.out.println("ACmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "b")
    public void processB(NodeComponent bindCmp) {
        System.out.println("BCmp executed!");
        throw new RuntimeException();
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.IS_CONTINUE_ON_ERROR, nodeId = "b")
    public boolean isContinueOnErrorB(NodeComponent bindCmp) {
        return true;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "b")
    public void rollbackB(NodeComponent bindCmp) throws Exception {
        System.out.println("BCmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "c")
    public void processC(NodeComponent bindCmp) {
        System.out.println("CCmp executed!");
    }


    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "d")
    public void processD(NodeComponent bindCmp) {
        System.out.println("DCmp executed!");
        throw new RuntimeException();
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "d")
    public void rollbackD(NodeComponent bindCmp) throws Exception {
        System.out.println("DCmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "e")
    public void processE(NodeComponent bindCmp) {
        System.out.println("ECmp executed!");
        throw new RuntimeException();
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "e")
    public void rollbackE() throws Exception {
        System.out.println("ECmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_SWITCH, nodeId = "f", nodeType = NodeTypeEnum.SWITCH)
    public String processF(NodeComponent bindCmp) {
        System.out.println("FCmp executed!");
        return "abc";
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "f", nodeType = NodeTypeEnum.SWITCH)
    public void rollbackF() throws Exception {
        System.out.println("FCmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_FOR, nodeId = "g", nodeType = NodeTypeEnum.FOR)
    public int processG(NodeComponent bindCmp) {
        System.out.println("GCmp executed!");
        return 3;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "g", nodeType = NodeTypeEnum.FOR)
    public void rollbackG() throws Exception {
        System.out.println("GCmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BREAK, nodeId = "h", nodeType = NodeTypeEnum.BREAK)
    public int processH(NodeComponent bindCmp) {
        System.out.println("HCmp executed!");
        throw new RuntimeException();
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "h", nodeType = NodeTypeEnum.BREAK)
    public void rollbackH() throws Exception {
        System.out.println("HCmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_ITERATOR, nodeId = "i", nodeType = NodeTypeEnum.ITERATOR)
    public Iterator<?> processI(NodeComponent bindCmp) {
        List<String> list = ListUtil.toList("jack", "mary", "tom");
        return list.iterator();
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "i", nodeType = NodeTypeEnum.ITERATOR)
    public void rollbackI() throws Exception {
        System.out.println("ICmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_WHILE, nodeId = "w", nodeType = NodeTypeEnum.WHILE)
    public boolean processW(NodeComponent bindCmp) {
        System.out.println("WCmp executed!");
        return true;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "w", nodeType = NodeTypeEnum.WHILE)
    public void rollbackW() throws Exception {
        System.out.println("WCmp rollback!");
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_IF, nodeId = "x", nodeType = NodeTypeEnum.IF)
    public boolean processX(NodeComponent bindCmp) {
        System.out.println("XCmp executed!");
        return true;
    }

    @LiteflowMethod(value = LiteFlowMethodEnum.ROLLBACK, nodeId = "x", nodeType = NodeTypeEnum.IF)
    public void rollbackX() throws Exception {
        System.out.println("XCmp rollback!");
    }

}
