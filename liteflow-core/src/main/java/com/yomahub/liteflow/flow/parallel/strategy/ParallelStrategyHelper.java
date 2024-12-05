package com.yomahub.liteflow.flow.parallel.strategy;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import com.yomahub.liteflow.enums.ParallelStrategyEnum;
import com.yomahub.liteflow.exception.ParallelExecutorCreateException;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

import java.util.Map;

/**
 * WHEN 并发策略辅助
 *
 * @author luo yi
 * @since 2.11.0
 */
public class ParallelStrategyHelper {

    private final LFLog LOG = LFLoggerManager.getLogger(ParallelStrategyHelper.class);

    /**
     * 此处使用Map缓存线程池信息 key - 线程池构建者的Class全类名 value - 线程池对象
     */
    private final Map<ParallelStrategyEnum, ParallelStrategyExecutor> strategyExecutorMap;

    private ParallelStrategyHelper() {
        strategyExecutorMap = MapUtil.newConcurrentHashMap();
    }

    /**
     * 使用静态内部类实现单例模式
     */
    private static class Holder {

        static final ParallelStrategyHelper INSTANCE = new ParallelStrategyHelper();

    }

    public static ParallelStrategyHelper loadInstance() {
        return ParallelStrategyHelper.Holder.INSTANCE;
    }

    private ParallelStrategyExecutor getParallelStrategyExecutor(ParallelStrategyEnum parallelStrategyEnum) {
        try {
            ParallelStrategyExecutor strategyExecutor = strategyExecutorMap.get(parallelStrategyEnum);
            if (ObjUtil.isNotNull(strategyExecutor)) return strategyExecutor;

            Class<ParallelStrategyExecutor> executorClass = (Class<ParallelStrategyExecutor>) Class.forName(parallelStrategyEnum.getClazz().getName());
            strategyExecutor = ContextAwareHolder.loadContextAware().registerBean(executorClass);
            strategyExecutorMap.put(parallelStrategyEnum, strategyExecutor);
            return strategyExecutor;
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw new ParallelExecutorCreateException(e.getMessage());
        }
    }

    public ParallelStrategyExecutor buildParallelExecutor(ParallelStrategyEnum parallelStrategyEnum) {
        if (ObjUtil.isNull(parallelStrategyEnum)) return buildParallelExecutor();
        return getParallelStrategyExecutor(parallelStrategyEnum);
    }

    /**
     * 默认需完成所有任务
     * @return ParallelStrategyExecutor
     */
    public ParallelStrategyExecutor buildParallelExecutor() {
        return buildParallelExecutor(ParallelStrategyEnum.ALL);
    }

    public void clearStrategyExecutorMap() {
        if (MapUtil.isNotEmpty(strategyExecutorMap)) {
            strategyExecutorMap.clear();
        }
    }

}
