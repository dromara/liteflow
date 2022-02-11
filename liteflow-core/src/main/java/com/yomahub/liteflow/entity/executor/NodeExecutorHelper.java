package com.yomahub.liteflow.entity.executor;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.google.common.collect.Maps;
import com.yomahub.liteflow.util.SpringAware;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * 节点执行器帮助器
 *
 * @author sikadai
 * @date 2022/1/24 19:00
 */
public class NodeExecutorHelper {
    /**
     * 此处使用Map缓存线程池信息
     * key - 节点执行器类Class全名
     * value - 节点执行器对象
     */
    private final Map<Class<? extends NodeExecutor>, NodeExecutor> nodeExecutorMap;

    private NodeExecutorHelper() {
        nodeExecutorMap = Maps.newConcurrentMap();
    }

    /**
     * 使用静态内部类实现单例模式
     */
    private static class Holder {
        static final NodeExecutorHelper INSTANCE = new NodeExecutorHelper();
    }

    /**
     * 获取帮助者的实例
     */
    public static NodeExecutorHelper loadInstance() {
        // 外围类能直接访问内部类（不管是否是静态的）的私有变量
        return Holder.INSTANCE;
    }

    /**
     * 单例模式驱动-通过调用该方法构建节点执行器
     */
    /**
     * 单例模式驱动-通过调用该方法构建节点执行器
     * 若nodeExecutorClass为空，则会使用默认的节点执行器
     *
     * @param nodeExecutorClass : 节点执行器的Class
     * @return
     */
    public NodeExecutor buildNodeExecutor(Class<? extends NodeExecutor> nodeExecutorClass) {
        // 高频操作-采取apache判空操作-效率高于hutool的isBlank将近3倍
        if (ObjectUtil.isNull(nodeExecutorClass)) {
            // 此处使用默认的节点执行器进行执行
            nodeExecutorClass = DefaultNodeExecutor.class;
        }
        NodeExecutor nodeExecutor = nodeExecutorMap.get(nodeExecutorClass);
        // 此处无需使用同步锁进行同步-因为即使同时创建了两个实例，但是添加到缓存中的只会存在一个且不会存在并发问题-具体是由ConcurrentMap保证
        if (ObjectUtil.isNull(nodeExecutor)) {
            // 获取重试执行器实例
            nodeExecutor = SpringAware.registerBean(nodeExecutorClass);
            // 缓存
            nodeExecutorMap.put(nodeExecutorClass, nodeExecutor);
        }
        return nodeExecutorMap.get(nodeExecutorClass);
    }
}
