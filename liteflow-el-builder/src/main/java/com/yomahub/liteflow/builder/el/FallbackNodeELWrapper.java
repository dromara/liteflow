package com.yomahub.liteflow.builder.el;

import cn.hutool.core.util.StrUtil;

/**
 * 降级节点表示
 *
 * @author luo yi
 * @since 2.12.3
 */
public class FallbackNodeELWrapper extends CommonNodeELWrapper {

    public FallbackNodeELWrapper(String nodeId) {
        super(nodeId);
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        FallbackNodeELWrapper fallbackNodeElWrapper = (FallbackNodeELWrapper) this.getFirstWrapper();
        StringBuilder sb = new StringBuilder();
        processWrapperTabs(sb, depth);
        sb.append(StrUtil.format("node(\"{}\")", fallbackNodeElWrapper.getNodeId()));
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }

}
