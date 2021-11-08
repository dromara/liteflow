package com.yomahub.liteflow.asynctool.exception;

/**
 * 代码来自于asyncTool,请参考：https://gitee.com/jd-platform-opensource/asyncTool
 * 如果任务在执行之前，自己后面的任务已经执行完或正在被执行，则抛该exception
 * @author wuweifeng wrote on 2020-02-18
 * @version 1.0
 */
public class SkippedException extends RuntimeException {
    public SkippedException() {
        super();
    }

    public SkippedException(String message) {
        super(message);
    }
}
