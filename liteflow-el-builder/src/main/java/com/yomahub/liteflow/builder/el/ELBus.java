package com.yomahub.liteflow.builder.el;

import java.util.Arrays;

/**
 * ELBus
 * 构建EL表达式的入口类
 *
 * @author gezuao
 * @since 2.11.1
 */
public class ELBus {

    public static final String TAB = "\t";

    /**
     * 创建 then 串行组件
     * 参数为ELWrapper
     * @param elWrappers
     * @return
     */
    public static ThenELWrapper then(ELWrapper ... elWrappers){
        checkNotBooleanArgs(elWrappers);
        return new ThenELWrapper(elWrappers);
    }

    public static ThenELWrapper then(Object ... objects){
        ELWrapper[] elWrappers = convertToNonLogicOpt(objects);
        return new ThenELWrapper(elWrappers);
    }

    /**
     * 创建 when 并行组件
     * @param elWrappers
     * @return
     */
    public static WhenELWrapper when(ELWrapper ... elWrappers){
        checkNotBooleanArgs(elWrappers);
        return new WhenELWrapper(elWrappers);
    }

    public static WhenELWrapper when(Object ... objects){
        ELWrapper[] elWrappers = convertToNonLogicOpt(objects);
        return new WhenELWrapper(elWrappers);
    }

    /**
     * 创建 if 条件判断表达式
     * @param ifELWrapper
     * @param trueELWrapper
     * @param falseELWrapper
     * @return
     */
    public static IfELWrapper ifOpt(NodeELWrapper ifELWrapper, Object trueELWrapper, Object falseELWrapper){
        return new IfELWrapper(ifELWrapper, convertToNonLogicOpt(trueELWrapper), convertToNonLogicOpt(falseELWrapper));
    }

    public static IfELWrapper ifOpt(String ifELWrapper, Object trueELWrapper, Object falseELWrapper){
        return new IfELWrapper((NodeELWrapper) convertToLogicOpt(ifELWrapper), convertToNonLogicOpt(trueELWrapper), convertToNonLogicOpt(falseELWrapper));
    }

    public static IfELWrapper ifOpt(AndELWrapper ifELWrapper, Object trueELWrapper, Object falseELWrapper){
        return new IfELWrapper(ifELWrapper, convertToNonLogicOpt(trueELWrapper), convertToNonLogicOpt(falseELWrapper));
    }

    public static IfELWrapper ifOpt(OrELWrapper ifELWrapper, Object trueELWrapper, Object falseELWrapper){
        return new IfELWrapper(ifELWrapper, convertToNonLogicOpt(trueELWrapper), convertToNonLogicOpt(falseELWrapper));
    }

    public static IfELWrapper ifOpt(NotELWrapper ifELWrapper, Object trueELWrapper, Object falseELWrapper){
        return new IfELWrapper(ifELWrapper, convertToNonLogicOpt(trueELWrapper), convertToNonLogicOpt(falseELWrapper));
    }

    public static IfELWrapper ifOpt(NodeELWrapper ifELWrapper, Object trueELWrapper){
        return new IfELWrapper(ifELWrapper, convertToNonLogicOpt(trueELWrapper));
    }

    public static IfELWrapper ifOpt(String ifELWrapper, Object trueELWrapper){
        return new IfELWrapper((NodeELWrapper) convertToLogicOpt(ifELWrapper), convertToNonLogicOpt(trueELWrapper));
    }

    public static IfELWrapper ifOpt(AndELWrapper ifELWrapper, Object trueELWrapper){
        return new IfELWrapper(ifELWrapper, convertToNonLogicOpt(trueELWrapper));
    }

    public static IfELWrapper ifOpt(OrELWrapper ifELWrapper, Object trueELWrapper){
        return new IfELWrapper(ifELWrapper, convertToNonLogicOpt(trueELWrapper));
    }

    public static IfELWrapper ifOpt(NotELWrapper ifELWrapper, Object trueELWrapper){
        return new IfELWrapper(ifELWrapper, convertToNonLogicOpt(trueELWrapper));
    }

    /**
     * 创建 node 单节点表达式
     * @param nodeId
     * @return
     */
    public static NodeELWrapper node(String nodeId){
        return new NodeELWrapper(nodeId);
    }

    /**
     * 创建 switch 选择表达式
     * @param nodeELWrapper
     * @return
     */
    public static SwitchELWrapper switchOpt(NodeELWrapper nodeELWrapper){
        return new SwitchELWrapper(nodeELWrapper);
    }

    public static SwitchELWrapper switchOpt(String nodeELWrapper){
        return new SwitchELWrapper(convert(nodeELWrapper));
    }

    /**
     * 创建 for 次数循环表达式
     * @param loopNumber
     * @return
     */
    public static ForELWrapper forOpt(Integer loopNumber){
        return new ForELWrapper(loopNumber, "FOR");
    }

    public static ForELWrapper forOpt(NodeELWrapper nodeELWrapper){
        return new ForELWrapper(nodeELWrapper, "FOR");
    }

    public static ForELWrapper forOpt(String nodeELWrapper){
        return new ForELWrapper(convert(nodeELWrapper), "FOR");
    }

    /**
     * 创建 while 条件循环表达式
     * @param nodeELWrapper
     * @return
     */
    public static WhileELWrapper whileOpt(NodeELWrapper nodeELWrapper){
        return new WhileELWrapper(nodeELWrapper, "WHILE");
    }

    public static WhileELWrapper whileOpt(String nodeELWrapper){
        return new WhileELWrapper(convert(nodeELWrapper), "WHILE");
    }

    public static WhileELWrapper whileOpt(AndELWrapper nodeELWrapper){
        return new WhileELWrapper(nodeELWrapper, "WHILE");
    }

    public static WhileELWrapper whileOpt(OrELWrapper nodeELWrapper){
        return new WhileELWrapper(nodeELWrapper, "WHILE");
    }

    public static WhileELWrapper whileOpt(NotELWrapper nodeELWrapper){
        return new WhileELWrapper(nodeELWrapper, "WHILE");
    }

    /**
     * 创建迭代循环表达式
     * @param nodeELWrapper
     * @return
     */
    public static IteratorELWrapper iteratorOpt(NodeELWrapper nodeELWrapper){
        return new IteratorELWrapper(nodeELWrapper, "ITERATOR");
    }

    public static IteratorELWrapper iteratorOpt(String nodeELWrapper){
        return new IteratorELWrapper(convert(nodeELWrapper), "ITERATOR");
    }

    /**
     * 创建捕获异常表达式
     * @param object
     * @return
     */
    public static CatchELWrapper catchException(Object object){
        return new CatchELWrapper(convertToNonLogicOpt(object));
    }

    /**
     * 创建与表达式
     * @param objects
     * @return
     */
    public static AndELWrapper and(Object ... objects){
        ELWrapper[] elWrappers = convertToLogicOpt(objects);
        return new AndELWrapper(elWrappers);
    }

    /**
     * 创建或表达式
     * @param objects
     * @return
     */
    public static OrELWrapper or(Object ... objects){
        ELWrapper[] elWrappers = convertToLogicOpt(objects);
        return new OrELWrapper(elWrappers);
    }

    /**
     * 创建非表达式
     * @param notElWrapper
     * @return
     */
    public static NotELWrapper not(NodeELWrapper notElWrapper){
        return new NotELWrapper(notElWrapper);
    }

    public static NotELWrapper not(String notElWrapper){
        return new NotELWrapper(convert(notElWrapper));
    }

    public static NotELWrapper not(AndELWrapper notElWrapper){
        return new NotELWrapper(notElWrapper);
    }

    public static NotELWrapper not(OrELWrapper notElWrapper){
        return new NotELWrapper(notElWrapper);
    }

    public static NotELWrapper not(NotELWrapper notElWrapper){
        return new NotELWrapper(notElWrapper);
    }

    /**
     * 参数转换并校验参数是否为ELWrapper类型或者String类型
     * @param objects
     * @return
     */
    public static ELWrapper[] convert(Object... objects){
        return Arrays.stream(objects).map(o -> {
            if (o instanceof String) {
                return new NodeELWrapper(o.toString());
            } else if (o instanceof ELWrapper) {
                return (ELWrapper) o;
            } else {
                throw new RuntimeException("param is error");
            }
        }).toArray(ELWrapper[]::new);
    }

    public static ELWrapper convert(Object object){
        if (object instanceof String) {
            return new NodeELWrapper(object.toString());
        } else if (object instanceof ELWrapper) {
            return (ELWrapper) object;
        } else {
            throw new RuntimeException("param is error");
        }
    }

    /**
     * 参数校验 只包含与或非的组件
     * @param objects
     * @return
     */
    public static ELWrapper[] convertToLogicOpt(Object... objects){
        ELWrapper[] elWrappers = convert(objects);
        checkBooleanArgs(elWrappers);
        return elWrappers;
    }

    public static ELWrapper convertToLogicOpt(Object object){
        ELWrapper elWrapper = convert(object);
        checkBooleanArgs(elWrapper);
        return elWrapper;
    }

    /**
     * 参数校验 不包含与或非表达式的组件
     * @param objects
     * @return
     */
    public static ELWrapper[] convertToNonLogicOpt(Object ... objects){
        ELWrapper[] elWrappers = convert(objects);
        checkNotBooleanArgs(elWrappers);
        return elWrappers;
    }

    public static ELWrapper convertToNonLogicOpt(Object object){
        ELWrapper elWrapper = convert(object);
        checkNotBooleanArgs(elWrapper);
        return elWrapper;
    }

    /**
     * 检查参数都不返回boolean值
     * @param elWrappers
     */
    public static void checkNotBooleanArgs(ELWrapper ... elWrappers) {
        for(ELWrapper elWrapper : elWrappers){
            if(elWrapper instanceof AndELWrapper){
                throw new RuntimeException("param is error");
            } else if(elWrapper instanceof OrELWrapper){
                throw new RuntimeException("param is error");
            } else if(elWrapper instanceof NotELWrapper){
                throw new RuntimeException("param is error");
            }
        }
    }

    /**
     * 检查参数是否都能返回boolean值
     * @param elWrappers
     */
    public static void checkBooleanArgs(ELWrapper ... elWrappers) {
        for(ELWrapper elWrapper : elWrappers){
            if(!(elWrapper instanceof AndELWrapper)
            && !(elWrapper instanceof OrELWrapper)
            && !(elWrapper instanceof NotELWrapper)
            && !(elWrapper instanceof NodeELWrapper)){
                throw new RuntimeException("param is error");
            }
        }
    }
}
