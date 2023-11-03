package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.common.ChainConstant;

import java.util.concurrent.TimeUnit;

/**
 * EL 规则中的 maxWaitMilliseconds 的操作符
 *
 * @author Kugaaa
 * @since 2.11.1
 */
public class MaxWaitMillisecondsOperator extends MaxWaitTimeOperator {

    @Override
    TimeUnit getMaxWaitTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    String operatorName() {
        return ChainConstant.MAX_WAIT_MILLISECONDS;
    }
}
