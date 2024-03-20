package com.yomahub.liteflow.flow.parallel;

import java.util.concurrent.*;
import java.util.function.Function;

/**
 * java8中CompletableFuture异步处理超时的方法
 *
 * Java 8 的 CompletableFuture 并没有 timeout 机制，虽然可以在 get 的时候指定 timeout，是一个同步堵塞的操作。怎样让
 * timeout 也是异步的呢？Java 8 内有内建的机 制支持，一般的实现方案是启动一个 ScheduledThreadpoolExecutor 线程在 timeout
 * 时间后直接调用 CompletableFuture.completeExceptionally(new TimeoutException())，
 * 然后用acceptEither() 或者 applyToEither 看是先计算完成还是先超时：
 *
 * 在 java 9 引入了 orTimeout 和 completeOnTimeOut 两个方法支持 异步 timeout 机制：
 *
 * public CompletableFuture orTimeout(long timeout, TimeUnit unit) : completes the
 * CompletableFuture with a TimeoutException after the specified timeout has elapsed.
 * public CompletableFuture completeOnTimeout(T value, long timeout, TimeUnit unit) :
 * provides a default value in the case that the CompletableFuture pipeline times out.
 * 内部实现上跟我们上面的实现方案是一模一样的，只是现在不需要自己实现了。
 *
 * 实际上hystrix等熔断的框架，其实现线程Timeout之后就关闭线程，也是基于同样的道理，所以我们可以看到hystrix中会有一个Timer Thread
 *
 * @author luliang
 * @since 2.6.4
 */
public class CompletableFutureTimeout {

	static final class Delayer {

		static ScheduledFuture<?> delay(Runnable command, long delay, TimeUnit unit) {
			return delayer.schedule(command, delay, unit);
		}

		static final class DaemonThreadFactory implements ThreadFactory {

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setName("CompletableFutureDelayScheduler");
				return t;
			}

		}

		static final ScheduledThreadPoolExecutor delayer;

		// 注意，这里使用一个线程就可以搞定 因为这个线程并不真的执行请求 而是仅仅抛出一个异常
		static {
			delayer = new ScheduledThreadPoolExecutor(1, new CompletableFutureTimeout.Delayer.DaemonThreadFactory());
			delayer.setRemoveOnCancelPolicy(true);
		}

	}

	public static <T> CompletableFuture<T> timeoutAfter(long timeout, TimeUnit unit) {
		CompletableFuture<T> result = new CompletableFuture<T>();
		// timeout 时间后 抛出TimeoutException 类似于sentinel / watcher
		CompletableFutureTimeout.Delayer.delayer.schedule(() -> result.completeExceptionally(new TimeoutException()),
				timeout, unit);
		return result;
	}

	// 哪个先完成 就apply哪一个结果 这是一个关键的API,exceptionally出现异常后返回默认值
	public static <T> CompletableFuture<T> completeOnTimeout(CompletableFuture<T> future, long timeout, TimeUnit unit, T t) {
		final CompletableFuture<T> timeoutFuture = timeoutAfter(timeout, unit);
		return future.applyToEither(timeoutFuture, Function.identity()).exceptionally((throwable) -> t);
	}

}
