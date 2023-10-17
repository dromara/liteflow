package com.yomahub.liteflow.builder.el;

import com.yomahub.liteflow.util.JsonUtil;

import java.util.Map;

/**
 * 条件循环表达式
 * WHILE只允许一个参数 参数为 返回布尔值的EL表达式
 * DO只允许一个参数 参数类型不为与或非表达式
 * 支持调用break方法，参数为与或非表达式或返回true false的单节点
 *
 * 支持设置 id tag data maxWaitSeconds 以及 parallel 属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class WhileELWrapper extends LoopELWrapper {
    public WhileELWrapper(ELWrapper elWrapper, String loopFunction){
        super(elWrapper, loopFunction);
    }

    public WhileELWrapper parallel(boolean parallel){
        setParallel(parallel);
        return this;
    }

    @Override
    public WhileELWrapper doOpt(Object object){
        ELWrapper elWrapper = ELBus.convertToNonLogicOpt(object);
        super.addWrapper(elWrapper, 1);
        return this;
    }

    public WhileELWrapper breakOpt(Object object){
        ELWrapper elWrapper = ELBus.convertToLogicOpt(object);
        super.addWrapper(elWrapper, 2);
        return this;
    }

    @Override
    public WhileELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public WhileELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public WhileELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }
}
