package com.yomahub.liteflow.builder.el;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.Map;

/**
 * 选择组件
 * SWITCH(a).TO(b,c,d...).default(x)
 * SWITCH只允许单个参数，类型为非与或非表达式
 * TO允许任意个参数，类型为非与或非表达式
 * DEFAULT只允许单个参数，类型为非与或非表达式
 *
 * 支持定义 id tag data maxWaitSeconds 属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class SwitchELWrapper extends ELWrapper{
    /**
     * default语句的表达式
     */
    private ELWrapper defaultELWrapper;

    public SwitchELWrapper(ELWrapper elWrapper){
        this.addWrapper(elWrapper, 0);
    }

    public SwitchELWrapper to(Object... objects){
        ELWrapper[] elWrappers = ELBus.convertToNonLogicOpt(objects);
        this.addWrapper(elWrappers);
        return this;
    }

    public SwitchELWrapper defaultOpt(Object object){
        defaultELWrapper = ELBus.convertToNonLogicOpt(object);
        return this;
    }

    @Override
    public SwitchELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public SwitchELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public SwitchELWrapper data(String dataName, Object object) {
        setData(JSONUtil.toJsonStr(object));
        setDataName(dataName);
        return this;
    }

    @Override
    public SwitchELWrapper data(String dataName, String jsonString) {
        try {
            JSONUtil.parseObj(jsonString);
        } catch (Exception e){
            throw new RuntimeException("字符串不符合Json格式！");
        }
        setData(jsonString);
        setDataName(dataName);
        return this;
    }

    @Override
    public SwitchELWrapper data(String dataName, Map<String, Object> jsonMap) {
        setData(JSONUtil.toJsonStr(jsonMap));
        setDataName(dataName);
        return this;
    }

    @Override
    public SwitchELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        processWrapperTabs(sb, depth);
        sb.append(StrUtil.format("SWITCH({})", this.getFirstWrapper().toEL(null, paramContext)));
        // 处理子表达式输出
        if(this.getElWrapperList().size() > 1) {
            sb.append(".TO(");
            processWrapperNewLine(sb, depth);
            for (int i = 1; i < this.getElWrapperList().size(); i++) {
                sb.append(this.getElWrapperList().get(i).toEL(sonDepth, paramContext));
                if (i != this.getElWrapperList().size() - 1) {
                    sb.append(",");
                    processWrapperNewLine(sb, depth);
                }
            }
            processWrapperNewLine(sb, depth);
            processWrapperTabs(sb, depth);
            sb.append(")");
        }
        // default可以不存在
        if(defaultELWrapper != null){
            sb.append(".DEFAULT(");
            processWrapperNewLine(sb, depth);
            sb.append(defaultELWrapper.toEL(sonDepth, paramContext));
            processWrapperNewLine(sb, depth);
            processWrapperTabs(sb, depth);
            sb.append(")");
        }

        // 处理表达式的共有属性
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }
}
