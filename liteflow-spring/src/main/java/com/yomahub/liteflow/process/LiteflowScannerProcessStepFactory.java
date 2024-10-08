package com.yomahub.liteflow.process;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.process.impl.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * spring bean finder 工厂
 *
 * @author tkc
 * @since 2.12.4
 */
public class LiteflowScannerProcessStepFactory {
    private static final List<LiteflowScannerProcessStep> SPRING_BEAN_FINDER = new ArrayList<>();

    public LiteflowScannerProcessStepFactory() {
        // 初始化
        register();
    }

    /**
     * 注册Spring Bean查找器
     * 该方法用于向Spring Bean查找器集合中添加各种不同类型的Bean查找器，并按照它们的优先级进行排序
     */
    private void register() {
        SPRING_BEAN_FINDER.add(new CmpAroundAspectBeanProcess());
        SPRING_BEAN_FINDER.add(new DeclWarpBeanProcess());
        SPRING_BEAN_FINDER.add(new NodeCmpBeanProcess());
        SPRING_BEAN_FINDER.add(new ScriptBeanProcess());
        SPRING_BEAN_FINDER.add(new ScriptMethodBeanProcess());

        // 按优先级排序
        CollUtil.sort(SPRING_BEAN_FINDER, Comparator.comparing(o -> o.type().getPriority()));
    }

    /**
     * 获取Spring Bean查找器链
     *
     * @return 返回一个包含SpringBeanFinder对象的列表，这些对象按照一定的顺序排列，
     * 用于查找和实例化Spring Beans
     */
    public Collection<LiteflowScannerProcessStep> getSteps() {
        return CollUtil.unmodifiable(SPRING_BEAN_FINDER);
    }
}
