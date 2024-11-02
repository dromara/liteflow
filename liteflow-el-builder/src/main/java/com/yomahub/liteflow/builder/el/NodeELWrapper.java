package com.yomahub.liteflow.builder.el;

import cn.hutool.core.util.StrUtil;

/**
 * 单节点表达式
 * 单节点也应以为一种表达式
 * 支持设置 tag data maxWaitSeconds 属性
 *
 * @author gezuao
 * @author luo yi
 * @since 2.11.1
 */
public class NodeELWrapper extends CommonNodeELWrapper {

    public NodeELWrapper(String nodeId) {
        super(nodeId);
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        NodeELWrapper nodeElWrapper = (NodeELWrapper) this.getFirstWrapper();
        StringBuilder sb = new StringBuilder();
        processWrapperTabs(sb, depth);
        sb.append(StrUtil.format("node(\"{}\")", nodeElWrapper.getNodeId()));
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }

}
