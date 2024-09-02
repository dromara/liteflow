package com.yomahub.liteflow.builder.el;

/**
 * 普通节点表示
 *
 * @author luo yi
 * @since 2.12.3
 */
public class CommonNodeELWrapper extends NodeELWrapper {

    public CommonNodeELWrapper(String nodeId) {
        super(nodeId);
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        CommonNodeELWrapper nodeElWrapper = (CommonNodeELWrapper) this.getFirstWrapper();
        StringBuilder sb = new StringBuilder();
        processWrapperTabs(sb, depth);
        sb.append(nodeElWrapper.getNodeId());
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }

}
