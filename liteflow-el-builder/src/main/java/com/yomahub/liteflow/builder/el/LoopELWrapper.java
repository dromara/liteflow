package com.yomahub.liteflow.builder.el;

import java.util.Map;

/**
 * FOR、WHILE、ITERATOR循环表达式的公共抽象父类
 *
 * @author gezuao
 * @author Bryan.Zhang
 * @since 2.11.1
 */
public class LoopELWrapper extends ELWrapper {

    public final static String FOR = "FOR";

    public final static String WHILE = "WHILE";

    public final static String ITERATOR = "ITERATOR";

    protected Integer loopNumber;

    /**
     * 以loopFunction变量进行区分FOR、WHILE、ITERATOR
     */
    protected String loopFunction;

    protected boolean parallel;

    public LoopELWrapper(Integer loopNumber, String loopFunction){
        this.loopNumber = loopNumber;
        this.loopFunction = loopFunction;
        this.addWrapper(null, 0);
    }

    public LoopELWrapper(ELWrapper elWrapper, String loopFunction){
        this.loopFunction = loopFunction;
        this.addWrapper(elWrapper, 0);
    }

    public LoopELWrapper parallel(boolean parallel){
        setParallel(parallel);
        return this;
    }

    public LoopELWrapper doOpt(Object object){
        ELWrapper elWrapper = ELBus.convertToNonBooleanOpt(object);
        this.addWrapper(elWrapper, 1);
        return this;
    }

    public LoopELWrapper breakOpt(Object object){
        ELWrapper elWrapper = ELBus.convertToBooleanOpt(object);
        super.addWrapper(elWrapper, 2);
        return this;
    }

    protected void setParallel(boolean parallel){
        this.parallel = parallel;
    }

    @Override
    public LoopELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public LoopELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public LoopELWrapper data(String dataName, Object object) {
        super.data(dataName, object);
        return this;
    }

    @Override
    public LoopELWrapper data(String dataName, String jsonString) {
        super.data(dataName, jsonString);
        return this;
    }

    @Override
    public LoopELWrapper data(String dataName, Map<String, Object> jsonMap) {
        super.data(dataName, jsonMap);
        return this;
    }

    @Override
    public LoopELWrapper bind(String key, String value) {
        super.bind(key, value);
        return this;
    }

    @Override
    public LoopELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    public LoopELWrapper retry(Integer count){
        super.retry(count);
        return this;
    }

    public LoopELWrapper retry(Integer count, String... exceptions){
        super.retry(count, exceptions);
        return this;
    }


    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        // 设置循坏组件的类型以及循环节点
        processWrapperTabs(sb, depth);
        sb.append(loopFunction).append("(");
        if(loopNumber != null){
            sb.append(loopNumber.toString());
        } else {
            processWrapperNewLine(sb, depth);
            sb.append(this.getElWrapperList().get(0).toEL(sonDepth, paramContext));
            processWrapperNewLine(sb, depth);
            processWrapperTabs(sb, depth);
        }
        sb.append(")");

        // 循环独有的并行语义
        if(this.parallel){
            sb.append(".parallel(true)");
        }

        // 设置循环组件输出
        if(this.getElWrapperList().size() > 1) {
            sb.append(".DO(");
            processWrapperNewLine(sb, depth);
            sb.append(this.getElWrapperList().get(1).toEL(sonDepth, paramContext));
            processWrapperNewLine(sb, depth);
            processWrapperTabs(sb, depth);
            sb.append(")");
        }

        // 设置退出循环组件输出
        if(this.getElWrapperList().size() > 2){
            sb.append(".BREAK(");
            processWrapperNewLine(sb, depth);
            sb.append(this.getElWrapperList().get(2).toEL(sonDepth, paramContext));
            processWrapperNewLine(sb, depth);
            processWrapperTabs(sb, depth);
            sb.append(")");
        }

        // 设置共有属性
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }
}
