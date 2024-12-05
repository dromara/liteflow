package com.yomahub.liteflow.flow.parallel;

import java.util.concurrent.*;
import java.util.function.BiConsumer;

public class CompletableFutureExpand {

    /**
     * 如果在给定超时之前未完成，则异常完成此 CompletableFuture 并抛出 {@link TimeoutException} 。
     *
     * @param timeout 在出现 TimeoutException 异常完成之前等待多长时间，以 {@code unit} 为单位
     * @param unit    一个 {@link TimeUnit}，结合 {@code timeout} 参数，表示给定粒度单位的持续时间
     * @param timeoutDefaultObj timeoutDefaultObj
     * @return 入参的 CompletableFuture
     */
    public static <T> CompletableFuture<T> completeOnTimeout(CompletableFuture<T> future, long timeout, TimeUnit unit, T timeoutDefaultObj) {
        if (future.isDone()) {
            return future;
        }

        return future.whenComplete(
                new Canceller(
                        Delayer.delay(
                                new Timeout<>(future, timeoutDefaultObj),
                                timeout,
                                unit
                        )
                )
        );
    }

    /**
     * 超时时异常完成的操作
     */
    static final class Timeout<T> implements Runnable {
        final CompletableFuture<T> future;

        final T timeoutDefaultObj;

        Timeout(CompletableFuture<T> future, T timeoutDefaultObj) {
            this.future = future;
            this.timeoutDefaultObj = timeoutDefaultObj;
        }

        public void run() {
            if (null != future && !future.isDone()) {
                future.complete(timeoutDefaultObj);
            }
        }
    }

    /**
     * 取消不需要的超时的操作
     */
    static final class Canceller implements BiConsumer<Object, Throwable> {
        final Future<?> future;

        Canceller(Future<?> future) {
            this.future = future;
        }

        public void accept(Object ignore, Throwable ex) {
            if (null == ex && null != future && !future.isDone()) {
                future.cancel(false);
            }
        }
    }

    /**
     * 单例延迟调度器，仅用于启动和取消任务，一个线程就足够
     */
    static final class Delayer {

        static final ScheduledThreadPoolExecutor delayer;

        static ScheduledFuture<?> delay(Runnable command, long delay, TimeUnit unit) {
            return delayer.schedule(command, delay, unit);
        }

        static final class DaemonThreadFactory implements ThreadFactory {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("CompletableFutureExpandUtilsDelayScheduler");
                return t;
            }
        }

        static {
            delayer = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory());
            delayer.setRemoveOnCancelPolicy(true);
        }
    }
}
