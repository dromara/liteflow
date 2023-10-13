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
     *
     * @param elWrappers 串行组件的子组件
     * @return {@link ThenELWrapper}
     */
    public static ThenELWrapper then(ELWrapper... elWrappers){
        checkNotBooleanArgs(elWrappers);
        return new ThenELWrapper(elWrappers);
    }

    public static ThenELWrapper then(Object ... objects){
        ELWrapper[] elWrappers = convertToNonLogicOpt(objects);
        return new ThenELWrapper(elWrappers);
    }

    /**
     * 创建 when 并行组件
     *
     * @param elWrappers 并行组件的子组件
     * @return {@link WhenELWrapper}
     */
    public static WhenELWrapper when(ELWrapper... elWrappers){
        checkNotBooleanArgs(elWrappers);
        return new WhenELWrapper(elWrappers);
    }

    public static WhenELWrapper when(Object ... objects){
        ELWrapper[] elWrappers = convertToNonLogicOpt(objects);
        return new WhenELWrapper(elWrappers);
    }


    /**
     * 创建 if 条件判断表达式
     *
     * @param ifElWrapper 判断节点
     * @param trueElWrapper 判断节点返回true时执行的组件
     * @param falseElWrapper 判断节点返回false时执行的组件
     * @return {@link IfELWrapper}
     */
    public static IfELWrapper ifOpt(NodeELWrapper ifElWrapper, Object trueElWrapper, Object falseElWrapper){
        return new IfELWrapper(ifElWrapper, convertToNonLogicOpt(trueElWrapper), convertToNonLogicOpt(falseElWrapper));
    }

    public static IfELWrapper ifOpt(String ifElWrapper, Object trueElWrapper, Object falseElWrapper){
        return new IfELWrapper((NodeELWrapper) convertToLogicOpt(ifElWrapper), convertToNonLogicOpt(trueElWrapper), convertToNonLogicOpt(falseElWrapper));
    }

    public static IfELWrapper ifOpt(AndELWrapper ifElWrapper, Object trueElWrapper, Object falseElWrapper){
        return new IfELWrapper(ifElWrapper, convertToNonLogicOpt(trueElWrapper), convertToNonLogicOpt(falseElWrapper));
    }

    public static IfELWrapper ifOpt(OrELWrapper ifElWrapper, Object trueElWrapper, Object falseElWrapper){
        return new IfELWrapper(ifElWrapper, convertToNonLogicOpt(trueElWrapper), convertToNonLogicOpt(falseElWrapper));
    }

    public static IfELWrapper ifOpt(NotELWrapper ifElWrapper, Object trueElWrapper, Object falseElWrapper){
        return new IfELWrapper(ifElWrapper, convertToNonLogicOpt(trueElWrapper), convertToNonLogicOpt(falseElWrapper));
    }

    public static IfELWrapper ifOpt(NodeELWrapper ifElWrapper, Object trueElWrapper){
        return new IfELWrapper(ifElWrapper, convertToNonLogicOpt(trueElWrapper));
    }

    public static IfELWrapper ifOpt(String ifElWrapper, Object trueElWrapper){
        return new IfELWrapper((NodeELWrapper) convertToLogicOpt(ifElWrapper), convertToNonLogicOpt(trueElWrapper));
    }

    public static IfELWrapper ifOpt(AndELWrapper ifElWrapper, Object trueElWrapper){
        return new IfELWrapper(ifElWrapper, convertToNonLogicOpt(trueElWrapper));
    }

    public static IfELWrapper ifOpt(OrELWrapper ifElWrapper, Object trueElWrapper){
        return new IfELWrapper(ifElWrapper, convertToNonLogicOpt(trueElWrapper));
    }

    public static IfELWrapper ifOpt(NotELWrapper ifElWrapper, Object trueElWrapper){
        return new IfELWrapper(ifElWrapper, convertToNonLogicOpt(trueElWrapper));
    }

    /**
     * 创建 node 单节点表达式
     *
     * @param nodeId 节点id
     * @return {@link NodeELWrapper}
     */
    public static NodeELWrapper node(String nodeId){
        return new NodeELWrapper(nodeId);
    }

    /**
     * 创建 switch 选择表达式
     *
     * @param nodeElWrapper 选择节点
     * @return {@link SwitchELWrapper}
     */
    public static SwitchELWrapper switchOpt(NodeELWrapper nodeElWrapper){
        return new SwitchELWrapper(nodeElWrapper);
    }

    public static SwitchELWrapper switchOpt(String nodeElWrapper){
        return new SwitchELWrapper(convert(nodeElWrapper));
    }

    /**
     * 创建 for 次数循环表达式
     *
     * @param loopNumber 循环次数
     * @return {@link ForELWrapper}
     */
    public static ForELWrapper forOpt(Integer loopNumber){
        return new ForELWrapper(loopNumber, "FOR");
    }

    /**
     * 创建 for 次数循环表达式
     *
     * @param nodeElWrapper 返回循环次数的节点
     * @return {@link ForELWrapper}
     */
    public static ForELWrapper forOpt(NodeELWrapper nodeElWrapper){
        return new ForELWrapper(nodeElWrapper, "FOR");
    }

    /**
     * 创建 for 次数循环表达式
     *
     * @param nodeElWrapper 返回循环次数的节点Id
     * @return {@link ForELWrapper}
     */
    public static ForELWrapper forOpt(String nodeElWrapper){
        return new ForELWrapper(convert(nodeElWrapper), "FOR");
    }

    /**
     * 创建 while 条件循环表达式
     *
     * @param nodeElWrapper 返回布尔值的节点
     * @return {@link WhileELWrapper}
     */
    public static WhileELWrapper whileOpt(NodeELWrapper nodeElWrapper){
        return new WhileELWrapper(nodeElWrapper, "WHILE");
    }

    public static WhileELWrapper whileOpt(String nodeElWrapper){
        return new WhileELWrapper(convert(nodeElWrapper), "WHILE");
    }

    public static WhileELWrapper whileOpt(AndELWrapper andElWrapper){
        return new WhileELWrapper(andElWrapper, "WHILE");
    }

    public static WhileELWrapper whileOpt(OrELWrapper orElWrapper){
        return new WhileELWrapper(orElWrapper, "WHILE");
    }

    public static WhileELWrapper whileOpt(NotELWrapper notElWrapper){
        return new WhileELWrapper(notElWrapper, "WHILE");
    }

    /**
     * 创建迭代循环表达式
     *
     * @param nodeElWrapper 迭代节点
     * @return {@link IteratorELWrapper}
     */
    public static IteratorELWrapper iteratorOpt(NodeELWrapper nodeElWrapper){
        return new IteratorELWrapper(nodeElWrapper, "ITERATOR");
    }

    public static IteratorELWrapper iteratorOpt(String nodeElWrapper){
        return new IteratorELWrapper(convert(nodeElWrapper), "ITERATOR");
    }

    /**
     * 创建捕获异常表达式
     *
     * @param object 可能产生异常的表达式
     * @return {@link CatchELWrapper}
     */
    public static CatchELWrapper catchException(Object object){
        return new CatchELWrapper(convertToNonLogicOpt(object));
    }

    /**
     * 创建与表达式
     *
     * @param objects 返回布尔值的表达式
     * @return {@link AndELWrapper}
     */
    public static AndELWrapper and(Object ... objects){
        ELWrapper[] elWrappers = convertToLogicOpt(objects);
        return new AndELWrapper(elWrappers);
    }

    /**
     * 创建或表达式
     *
     * @param objects 返回布尔值的表达式
     * @return {@link OrELWrapper}
     */
    public static OrELWrapper or(Object ... objects){
        ELWrapper[] elWrappers = convertToLogicOpt(objects);
        return new OrELWrapper(elWrappers);
    }

    /**
     * 创建非表达式
     *
     * @param notElWrapper 返回布尔值的表达式
     * @return {@link NotELWrapper}
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
     *
     * @param objects 表达式或字符串
     * @return {@link ELWrapper[]}
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
     *
     * @param objects 表达式或字符串
     * @return {@link ELWrapper[]}
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
     *
     * @param objects 表达式或字符串
     * @return {@link ELWrapper[]}
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
     *
     * @param elWrappers 表达式
     */
    public static void checkNotBooleanArgs(ELWrapper... elWrappers) {
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
     *
     * @param elWrappers 表达式
     */
    public static void checkBooleanArgs(ELWrapper... elWrappers) {
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
