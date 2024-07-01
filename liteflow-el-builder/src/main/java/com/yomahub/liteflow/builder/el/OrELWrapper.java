package com.yomahub.liteflow.builder.el;

/**
 * 与或非表达式中的 或表达式
 * 参数允许任意数量 参数必须返回true或false
 *
 * 使用or()方法加入新的表达式
 * 支持设置 id tag data maxWaitSeconds 属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class OrELWrapper extends ELWrapper {

    public OrELWrapper(ELWrapper... elWrappers){
        this.addWrapper(elWrappers);
    }

    public OrELWrapper or(Object ... object){
        ELWrapper[] elWrapper = ELBus.convertToBooleanOpt(object);
        this.addWrapper(elWrapper);
        return this;
    }

    @Override
    public OrELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public OrELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        processWrapperTabs(sb, depth);
        sb.append("OR(");
        processWrapperNewLine(sb, depth);
        // 处理子表达式输出
        for (int i = 0; i < this.getElWrapperList().size(); i++) {
            if (i > 0){
                sb.append(",");
                processWrapperNewLine(sb, depth);
            }
            sb.append(this.getElWrapperList().get(i).toEL(sonDepth, paramContext));
        }
        processWrapperNewLine(sb, depth);
        processWrapperTabs(sb, depth);
        sb.append(")");

        // 处理公共属性输出
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }
}
