package com.yomahub.liteflow.builder.el;

import com.yomahub.liteflow.util.JsonUtil;

import java.util.Map;

/**
 * 次数循环表达式
 * FOR只允许一个参数 参数为 Integer 或 返回循环次数的 EL表达式
 * DO只允许一个参数 参数类型不为与或非表达式
 * 支持调用break方法，参数为与或非表达式或返回true false的单节点
 *
 * 支持设置 id tag data maxWaitSeconds 以及 parallel 属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class ForELWrapper extends LoopELWrapper{

    public ForELWrapper(Integer loopNumber, String loopFunction){
        super(loopNumber, loopFunction);
    }

    public ForELWrapper(ELWrapper elWrapper, String loopFunction){
        super(elWrapper, loopFunction);
    }

    @Override
    public ForELWrapper doOpt(Object object){
        ELWrapper elWrapper = ELBus.convertToNonLogicOpt(object);
        super.addWrapper(elWrapper, 1);
        return this;
    }

    public ForELWrapper breakOpt(Object object){
        ELWrapper elWrapper = ELBus.convertToLogicOpt(object);
        super.addWrapper(elWrapper, 2);
        return this;
    }

    public ForELWrapper parallel(boolean parallel){
        setParallel(parallel);
        return this;
    }

    @Override
    public ForELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public ForELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public ForELWrapper data(String dataName, Object object) {
        setData(JsonUtil.toJsonString(object));
        setDataName(dataName);
        return this;
    }

    @Override
    public ForELWrapper data(String dataName, String jsonString) {
        try {
            JsonUtil.parseObject(jsonString);
        } catch (Exception e){
            throw new RuntimeException("字符串不符合Json格式！");
        }
        setData(jsonString);
        setDataName(dataName);
        return this;
    }

    @Override
    public ForELWrapper data(String dataName, Map<String, Object> jsonMap) {
        setData(JsonUtil.toJsonString(jsonMap));
        setDataName(dataName);
        return this;
    }

    @Override
    public ForELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }
}
