package com.yomahub.liteflow.agent.hitl;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/** Executes an initial ReAct call and an optional HITL continuation as one transaction. */
public final class ReActCallExecutor {

    private final Supplier<Instant> now;
    private final Scheduler timeoutScheduler;

    public ReActCallExecutor() {
        this(Instant::now, Schedulers.parallel());
    }

    ReActCallExecutor(Supplier<Instant> now, Scheduler timeoutScheduler) {
        this.now = Objects.requireNonNull(now, "now");
        this.timeoutScheduler = Objects.requireNonNull(timeoutScheduler, "timeoutScheduler");
    }

    public Mono<Msg> execute(
            ReActAgent agent,
            List<Msg> input,
            AgentOutputSpec output,
            RuntimeContext runtimeContext,
            LiteFlowAgentContext context,
            AgentConfirmationHandler handler,
            Duration confirmationTimeout,
            boolean failOnDeniedTool,
            Duration cleanupTimeout) {
        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(runtimeContext, "runtimeContext");
        Objects.requireNonNull(context, "context");
        requirePositive(confirmationTimeout, "confirmationTimeout");
        requirePositive(cleanupTimeout, "cleanupTimeout");
        AgentCall call = agentCall(agent, output, runtimeContext);

        return awaitTermination(withRuntimeDeadline(
                        call.invoke(input), context))
                .flatMap(reply -> reply.getGenerateReason() == GenerateReason.PERMISSION_ASKING
                        ? continueAfterConfirmation(
                                call,
                                context,
                                reply,
                                handler,
                                confirmationTimeout,
                                failOnDeniedTool,
                                cleanupTimeout)
                        : Mono.just(reply));
    }

    private Mono<Msg> continueAfterConfirmation(
            AgentCall call,
            LiteFlowAgentContext context,
            Msg reply,
            AgentConfirmationHandler handler,
            Duration confirmationTimeout,
            boolean failOnDeniedTool,
            Duration cleanupTimeout) {
        List<io.agentscope.core.message.ToolUseBlock> pending =
                ConfirmationResultValidator.pendingTools(reply);
        ConfirmationRequest request;
        try {
            request = ConfirmationResultValidator.request(
                    reply, context.getConfirmationEvents());
        } catch (RuntimeException failure) {
            return cleanupThenFail(
                    call,
                    context,
                    pending,
                    permissionFailure("Invalid HITL confirmation protocol", failure),
                    cleanupTimeout);
        }
        if (handler == null) {
            return cleanupThenFail(
                    call,
                    context,
                    request.toolCalls(),
                    permissionFailure("No AgentConfirmationHandler is configured", null),
                    cleanupTimeout);
        }

        Mono<List<ConfirmResult>> decisions = invokeHandler(handler, request, context);
        return withConfirmationDeadline(decisions, context, confirmationTimeout)
                .switchIfEmpty(Mono.error(permissionFailure(
                        "AgentConfirmationHandler completed without results", null)))
                .onErrorMap(failure -> failure instanceof AgentInvocationException
                        ? failure
                        : permissionFailure("AgentConfirmationHandler failed", failure))
                .flatMap(results -> {
                    List<ConfirmResult> validated;
                    try {
                        validated = ConfirmationResultValidator.validateResults(request, results);
                    } catch (RuntimeException failure) {
                        return Mono.error(permissionFailure(
                                "Invalid HITL confirmation results", failure));
                    }
                    if (failOnDeniedTool
                            && validated.stream().anyMatch(result -> !result.isConfirmed())) {
                        return Mono.error(permissionFailure(
                                "A tool confirmation was denied", null));
                    }
                    return awaitTermination(withRuntimeDeadline(
                            call.invoke(resumeMessage(validated)),
                            context));
                })
                .onErrorResume(AgentInvocationException.class, failure ->
                        failure.getErrorType() == AgentInvocationErrorType.PERMISSION
                                || failure.getErrorType() == AgentInvocationErrorType.TIMEOUT
                                ? cleanupThenFail(
                                        call,
                                        context,
                                        request.toolCalls(),
                                        failure,
                                        cleanupTimeout)
                                : Mono.error(failure));
    }

    private Mono<List<ConfirmResult>> invokeHandler(
            AgentConfirmationHandler handler,
            ConfirmationRequest request,
            LiteFlowAgentContext context) {
        return Mono.defer(() -> {
            Mono<List<ConfirmResult>> result = handler.confirm(request.event(), context);
            if (result == null) {
                return Mono.error(new NullPointerException(
                        "AgentConfirmationHandler.confirm returned null"));
            }
            return result;
        });
    }

    private <T> Mono<T> withConfirmationDeadline(
            Mono<T> source,
            LiteFlowAgentContext context,
            Duration confirmationTimeout) {
        Duration runtimeRemaining = remaining(context);
        boolean runtimeWins = runtimeRemaining.compareTo(confirmationTimeout) <= 0;
        Duration timeout = runtimeWins ? runtimeRemaining : confirmationTimeout;
        TimeoutException marker = runtimeWins
                ? new RuntimeDeadlineExceededException()
                : new ConfirmationDeadlineExceededException(confirmationTimeout);
        if (timeout.isZero() || timeout.isNegative()) {
            context.cancel();
            return Mono.error(runtimeFailure(marker));
        }
        return source.timeout(timeout, Mono.error(marker), timeoutScheduler)
                .onErrorMap(RuntimeDeadlineExceededException.class, failure -> {
                    context.cancel();
                    return runtimeFailure(failure);
                })
                .onErrorMap(ConfirmationDeadlineExceededException.class, failure -> {
                    context.cancel();
                    return new AgentInvocationException(
                            AgentInvocationErrorType.TIMEOUT,
                            failure.getMessage(),
                            failure);
                });
    }

    private Mono<Msg> withRuntimeDeadline(
            Mono<Msg> source, LiteFlowAgentContext context) {
        Duration remaining = remaining(context);
        if (remaining.isZero() || remaining.isNegative()) {
            context.cancel();
            return Mono.error(runtimeFailure(new RuntimeDeadlineExceededException()));
        }
        return source.timeout(
                        remaining,
                        Mono.error(new RuntimeDeadlineExceededException()),
                        timeoutScheduler)
                .onErrorMap(RuntimeDeadlineExceededException.class, failure -> {
                    context.cancel();
                    return runtimeFailure(failure);
                });
    }

    private Mono<Msg> cleanupThenFail(
            AgentCall call,
            LiteFlowAgentContext context,
            List<io.agentscope.core.message.ToolUseBlock> pending,
            AgentInvocationException intendedFailure,
            Duration cleanupTimeout) {
        List<ConfirmResult> denied = ConfirmationResultValidator.denyAll(pending);
        Mono<Msg> cleanup = call.invoke(resumeMessage(denied))
                .timeout(
                        cleanupTimeout,
                        Mono.error(new CleanupDeadlineExceededException(cleanupTimeout)),
                        timeoutScheduler);
        return awaitTermination(cleanup)
                .then(Mono.<Msg>error(intendedFailure))
                .onErrorResume(cleanupFailure -> {
                    if (cleanupFailure != intendedFailure) {
                        intendedFailure.addSuppressed(cleanupFailure);
                    }
                    return Mono.<Msg>error(intendedFailure);
                });
    }

    private static Mono<Msg> awaitTermination(Mono<Msg> source) {
        if (source == null) {
            return Mono.error(new NullPointerException("ReActAgent.call returned null"));
        }
        return source.flux().collectList().flatMap(replies -> {
            if (replies.isEmpty()) {
                return Mono.empty();
            }
            if (replies.size() != 1) {
                return Mono.error(new IllegalStateException(
                        "ReActAgent.call emitted more than one reply"));
            }
            return Mono.just(replies.get(0));
        });
    }

    private static AgentCall agentCall(
            ReActAgent agent,
            AgentOutputSpec output,
            RuntimeContext runtimeContext) {
        return switch (output.kind()) {
            case TEXT -> input -> agent.call(input, runtimeContext);
            case JAVA_TYPE -> {
                Class<?> javaType = output.javaType();
                yield input -> agent.call(input, javaType, runtimeContext);
            }
            case JSON_SCHEMA -> {
                com.fasterxml.jackson.databind.JsonNode jsonSchema = output.jsonSchema();
                yield input -> agent.call(input, jsonSchema, runtimeContext);
            }
        };
    }

    private static List<Msg> resumeMessage(List<ConfirmResult> results) {
        return List.of(UserMessage.builder()
                .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, List.copyOf(results)))
                .build());
    }

    private Duration remaining(LiteFlowAgentContext context) {
        return Duration.between(now.get(), context.getDeadline());
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static AgentInvocationException permissionFailure(
            String message, Throwable cause) {
        return new AgentInvocationException(
                AgentInvocationErrorType.PERMISSION, message, cause);
    }

    private static AgentInvocationException runtimeFailure(Throwable cause) {
        return new AgentInvocationException(
                AgentInvocationErrorType.TIMEOUT,
                "Agent invocation exceeded runtime timeout",
                cause);
    }

    private static final class RuntimeDeadlineExceededException extends TimeoutException {
        private RuntimeDeadlineExceededException() {
            super("Agent invocation exceeded runtime timeout");
        }
    }

    private static final class ConfirmationDeadlineExceededException extends TimeoutException {
        private ConfirmationDeadlineExceededException(Duration timeout) {
            super("Agent confirmation exceeded timeout " + timeout);
        }
    }

    private static final class CleanupDeadlineExceededException extends TimeoutException {
        private CleanupDeadlineExceededException(Duration timeout) {
            super("Denied HITL cleanup exceeded timeout " + timeout);
        }
    }

    @FunctionalInterface
    private interface AgentCall {
        Mono<Msg> invoke(List<Msg> input);
    }
}
