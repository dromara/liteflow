package com.yomahub.liteflow.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.InstructionSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * LiteFlow上下文正则表达式匹配器
 * 用来根据正则表达式去寻找上下文中符合的对象
 * @author Bryan.Zhang
 * @since 2.13.0
 */
public class LiteflowContextRegexMatcher {

    private static final ExpressRunner expressRunner = new ExpressRunner();

    public static Object searchContext(List<Tuple> contextList, String regPattern){
        // 把上下文数据转换成map形式的，key为别名，value为上下文
        Map<String, Object> contextMap = contextList.stream().collect(
                Collectors.toMap(tuple -> tuple.get(0), tuple -> tuple.get(1))
        );

        List<String> errorList = new ArrayList<>();

        Object result = null;
        // 根据表达式去上下文里搜索相匹配的数据
        for(Map.Entry<String, Object> entry : contextMap.entrySet()){
            try{
                InstructionSet instructionSet = expressRunner.getInstructionSetFromLocalCache(entry.getKey() + "." + regPattern);
                DefaultContext<String, Object> context = new DefaultContext<>();
                context.put(entry.getKey(), entry.getValue());
                result = expressRunner.execute(instructionSet, context, errorList, false, false);
                if (result != null){
                    break;
                }
            }catch (Exception ignore){}
        }

        if (result == null){
            try{
                // 如果没有搜到，那么尝试推断表达式是指定的上下文，按照指定上下文的方式去再获取
                InstructionSet instructionSet = expressRunner.getInstructionSetFromLocalCache("contextMap." + regPattern);
                DefaultContext<String, Object> context = new DefaultContext<>();
                context.put("contextMap", contextMap);
                result = expressRunner.execute(instructionSet, context, errorList, false, false);
            }catch (Exception ignore){}
        }

        return result;
    }

    public static void searchAndSetContext(List<Tuple> contextList, String methodExpress, Object... args){
        // 把上下文数据转换成map形式的，key为别名，value为上下文
        Map<String, Object> contextMap = contextList.stream().collect(
                Collectors.toMap(tuple -> tuple.get(0), tuple -> tuple.get(1))
        );

        List<String> errorList = new ArrayList<>();

        boolean flag = false;

        String argStr = IntStream.range(0, args.length).mapToObj(
                idx -> StrUtil.format("arg{}", idx)
        ).collect(Collectors.joining(","));

        List<TupleOf2<String, Integer>> tupleList = IntStream.range(0, args.length).mapToObj(
                idx -> new TupleOf2<>(StrUtil.format("arg{}", idx), idx)
        ).collect(Collectors.toList());

        for(Map.Entry<String, Object> entry : contextMap.entrySet()){
            try{
                InstructionSet instructionSet = expressRunner.getInstructionSetFromLocalCache(StrUtil.format("{}.{}({})", entry.getKey(), methodExpress, argStr));
                DefaultContext<String, Object> context = new DefaultContext<>();
                context.put(entry.getKey(), entry.getValue());
                tupleList.forEach(tuple -> context.put(tuple.getA(), args[tuple.getB()]));
                expressRunner.execute(instructionSet, context, errorList, false, false);
                flag = true;
                break;
            }catch (Exception ignore){}
        }

        // 根据表达式去上下文里搜索相匹配的数据
        if (BooleanUtil.isFalse(flag)){
            try{
                // 如果没有搜到，那么尝试推断表达式是指定的上下文，按照指定上下文的方式去再获取
                InstructionSet instructionSet = expressRunner.getInstructionSetFromLocalCache(StrUtil.format("contextMap.{}({})", methodExpress, argStr));
                DefaultContext<String, Object> context = new DefaultContext<>();
                context.put("contextMap", contextMap);
                tupleList.forEach(tuple -> context.put(tuple.getA(), args[tuple.getB()]));
                expressRunner.execute(instructionSet, context, errorList, false, false);
            }catch (Exception ignore){}
        }
    }
}
