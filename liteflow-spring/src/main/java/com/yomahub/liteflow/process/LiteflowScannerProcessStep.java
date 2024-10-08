package com.yomahub.liteflow.process;

import com.yomahub.liteflow.process.context.LiteflowScannerProcessStepContext;
import com.yomahub.liteflow.process.enums.LiteflowScannerProcessStepEnum;

/**
 * liteflow scanner process step 接口
 *
 * @author tkc
 * @since 2.12.4
 */
public interface LiteflowScannerProcessStep {

    /**
     * 判断是否满足条件
     *
     * @param ctx 上下文
     */
    boolean filter(LiteflowScannerProcessStepContext ctx);

    /**
     * 对 bean 进行加工
     *
     * @param ctx 上下文
     */
    Object postProcessAfterInitialization(LiteflowScannerProcessStepContext ctx);

    /**
     * 类型
     */
    LiteflowScannerProcessStepEnum type();
}
