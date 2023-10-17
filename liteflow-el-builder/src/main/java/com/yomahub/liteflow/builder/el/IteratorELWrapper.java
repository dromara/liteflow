package com.yomahub.liteflow.builder.el;

import com.yomahub.liteflow.util.JsonUtil;

import java.util.Map;

/**
 * 迭代循环表达式
 * ITERATOR只允许一个参数 参数为 EL表达式
 * DO只允许一个参数 参数类型不为与或非表达式
 *
 * 支持设置 id tag data maxWaitSeconds 以及 parallel 属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class IteratorELWrapper extends LoopELWrapper {
    public IteratorELWrapper(ELWrapper elWrapper, String loopFunction){
        super(elWrapper, loopFunction);
    }

    public IteratorELWrapper parallel(boolean parallel){
        setParallel(parallel);
        return this;
    }

    @Override
    public IteratorELWrapper doOpt(Object object){
        ELWrapper elWrapper = ELBus.convertToNonLogicOpt(object);
        super.addWrapper(elWrapper, 1);
        return this;
    }

    @Override
    public IteratorELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public IteratorELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public IteratorELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }
}
