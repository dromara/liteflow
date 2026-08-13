package com.yomahub.liteflow.agent.a2a.server;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import io.agentscope.core.a2a.server.executor.runner.AgentRequestOptions;
import io.agentscope.core.a2a.server.executor.runner.AgentRunner;
import io.agentscope.core.message.Msg;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** A2A task registry that owns and terminates exactly one typed runtime per task. */
public final class LiteFlowA2aAgentRunner implements AgentRunner {

    private final A2aServerAgentFactory factory;
    private final InvocationIdentityResolver identityResolver;
    private final String agentKey;
    private final String defaultUserId;
    private final A2aProtocolEventAdapter eventAdapter = new A2aProtocolEventAdapter();
    private final ConcurrentMap<String, TaskHandle> tasks = new ConcurrentHashMap<>();

    public LiteFlowA2aAgentRunner(
            A2aServerAgentFactory factory,
            String namespace,
            String agentKey,
            String defaultUserId) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.identityResolver = new InvocationIdentityResolver(namespace);
        this.agentKey = requireText(agentKey, "agentKey");
        this.defaultUserId = requireText(defaultUserId, "defaultUserId");
    }

    @Override
    public String getAgentName() {
        return factory.agentName();
    }

    @Override
    public String getAgentDescription() {
        return factory.agentDescription();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Flux stream(List<Msg> requestMessages, AgentRequestOptions options) {
        Objects.requireNonNull(requestMessages, "requestMessages");
        AgentRequestOptions resolved = resolveOptions(options);
        String taskId = resolved.getTaskId();
        TaskHandle handle = new TaskHandle();
        if (tasks.putIfAbsent(taskId, handle) != null) {
            throw new IllegalStateException("A2A task is already active: " + taskId);
        }

        try {
            A2aServerAgentFactory.OwnedAgentRuntime runtime = Objects.requireNonNull(
                    factory.open(resolved), "A2A server factory returned null runtime");
            if (!handle.start(runtime)) {
                tasks.remove(taskId, handle);
                return Flux.error(new IllegalStateException(
                        "A2A task was stopped before runtime start: " + taskId));
            }
            Flux<?> events = eventAdapter.adapt(Objects.requireNonNull(
                    runtime.stream(List.copyOf(requestMessages)),
                    "A2A runtime returned null event stream"));
            return handle.guardSubscription(taskId, events)
                    .doFinally(ignored -> finish(taskId, handle, false));
        }
        catch (RuntimeException | LinkageError failure) {
            tasks.remove(taskId, handle);
            handle.finish(false);
            throw failure;
        }
    }

    @Override
    public void stop(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        TaskHandle handle = tasks.remove(taskId);
        if (handle != null) {
            handle.finish(true);
        }
    }

    private AgentRequestOptions resolveOptions(AgentRequestOptions options) {
        Objects.requireNonNull(options, "options");
        String taskId = requireText(options.getTaskId(), "taskId");
        String conversationId = requireText(options.getSessionId(), "sessionId");
        String userId = options.getUserId();
        if (userId == null || userId.isBlank()) {
            userId = defaultUserId;
        }
        AgentInvocationIdentity identity =
                identityResolver.resolve(userId, conversationId, agentKey);
        AgentRequestOptions resolved = new AgentRequestOptions();
        resolved.setTaskId(taskId);
        resolved.setUserId(userId);
        resolved.setSessionId(identity.runtimeSessionId());
        return resolved;
    }

    private void finish(String taskId, TaskHandle handle, boolean interrupt) {
        tasks.remove(taskId, handle);
        handle.finish(interrupt);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static final class TaskHandle {
        private final AtomicBoolean stopped = new AtomicBoolean();
        private final AtomicBoolean finished = new AtomicBoolean();
        private volatile A2aServerAgentFactory.OwnedAgentRuntime runtime;

        private synchronized boolean start(A2aServerAgentFactory.OwnedAgentRuntime runtime) {
            this.runtime = runtime;
            if (stopped.get()) {
                finish(true);
                return false;
            }
            return true;
        }

        private synchronized void finish(boolean interrupt) {
            if (interrupt) {
                stopped.set(true);
            }
            if (runtime == null || !finished.compareAndSet(false, true)) {
                return;
            }
            if (interrupt) {
                suppress(runtime::interrupt);
            }
            suppress(runtime::close);
        }

        private <T> Flux<T> guardSubscription(String taskId, Flux<T> events) {
            return Flux.from(subscriber -> subscribe(taskId, events, subscriber));
        }

        private synchronized <T> void subscribe(
                String taskId, Flux<T> events, Subscriber<? super T> subscriber) {
            if (finished.get()) {
                Flux.<T>error(new IllegalStateException(
                        "A2A task was stopped before subscription: " + taskId))
                        .subscribe(subscriber);
                return;
            }
            events.subscribe(subscriber);
        }

        private static void suppress(Runnable action) {
            try {
                action.run();
            }
            catch (RuntimeException | LinkageError ignored) {
                // Termination signal and registry cleanup remain authoritative.
            }
        }
    }
}
