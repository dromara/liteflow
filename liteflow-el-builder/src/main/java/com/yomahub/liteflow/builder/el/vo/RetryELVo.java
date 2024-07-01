package com.yomahub.liteflow.builder.el.vo;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用于ELBus中设置retry关键字的对象
 * @author Bryan.Zhang
 * @since 2.12.2
 */
public class RetryELVo {

    private int count;

    private String[] exceptions;

    public RetryELVo(int count) {
        this.count = count;
    }

    public RetryELVo(int count, String... exceptions) {
        this.count = count;
        this.exceptions = exceptions;
    }

    @Override
    public String toString() {
        if (exceptions == null) {
            return StrUtil.format("{}", count);
        }else{
            return StrUtil.format("{},{}", count, Arrays.stream(exceptions).map(s -> StrUtil.format("\"{}\"", s)).collect(Collectors.joining(",")));
        }
    }
}
