package com.yomahub.liteflow.agent.hitl;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolUseBlock;
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

/** Executes an initial Agent call and every required HITL continuation as one transaction. */
public final class AgentCallExecutor {

    private final Supplier<Instant> now;
    private final Scheduler timeoutScheduler;

    public AgentCallExecutor() {
        this(Instant::now, Schedulers.parallel());
    }

    AgentCallExecutor(Supplier<Instant> now, Scheduler timeoutScheduler) {
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
        return execute(
                new AgentCallTarget() {
                    @Override
                    public Mono<Msg> call(
                            List<Msg> messages, RuntimeContext context) {
                        return agent.call(messages, context);
                    }

                    @Override
                    public Mono<Msg> call(
                            List<Msg> messages,
                            Class<?> javaType,
                            RuntimeContext context) {
                        return agent.call(messages, javaType, context);
                    }

                    @Override
                    public Mono<Msg> call(
                            List<Msg> messages,
                            com.fasterxml.jackson.databind.JsonNode jsonSchema,
                            RuntimeContext context) {
                        return agent.call(messages, jsonSchema, context);
                    }
                },
                input,
                output,
                runtimeContext,
                context,
                handler,
                confirmationTimeout,
                failOnDeniedTool,
                cleanupTimeout);
    }

    public Mono<Msg> execute(
            AgentCallTarget target,
            List<Msg> input,
            AgentOutputSpec output,
            RuntimeContext runtimeContext,
            LiteFlowAgentContext context,
            AgentConfirmationHandler handler,
            Duration confirmationTimeout,
            boolean failOnDeniedTool,
            Duration cleanupTimeout) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(runtimeContext, "runtimeContext");
        Objects.requireNonNull(context, "context");
        requirePositive(confirmationTimeout, "confirmationTimeout");
        requirePositive(cleanupTimeout, "cleanupTimeout");
        AgentCall call = agentCall(target, output, runtimeContext);

        return Mono.defer(() -> {
            int eventOffset = confirmationEventCount(context);
            return awaitTermination(withRuntimeDeadline(call.invoke(input), context))
                    .flatMap(reply -> completeNormalRounds(
                            call,
                            context,
                            new NormalRound(reply, eventOffset),
                            handler,
                            confirmationTimeout,
                            failOnDeniedTool,
                            cleanupTimeout));
        });
    }

    private Mono<Msg> completeNormalRounds(
            AgentCall call,
            LiteFlowAgentContext context,
            NormalRound initialRound,
            AgentConfirmationHandler handler,
            Duration confirmationTimeout,
            boolean failOnDeniedTool,
            Duration cleanupTimeout) {
        return Mono.just(initialRound)
                .expand(round -> isAsking(round.reply())
                        ? advanceNormalRound(
                                call,
                                context,
                                round,
                                handler,
                                confirmationTimeout,
                                failOnDeniedTool,
                                cleanupTimeout)
                        : Mono.empty())
                .filter(round -> !isAsking(round.reply()))
                .next()
                .map(NormalRound::reply);
    }

    private Mono<NormalRound> advanceNormalRound(
            AgentCall call,
            LiteFlowAgentContext context,
            NormalRound round,
            AgentConfirmationHandler handler,
            Duration confirmationTimeout,
            boolean failOnDeniedTool,
            Duration cleanupTimeout) {
        Msg reply = round.reply();
        List<ToolUseBlock> pending = ConfirmationResultValidator.pendingTools(reply);
        ConfirmationRequest request;
        try {
            request = ConfirmationResultValidator.request(
                    reply, confirmationEventsSince(context, round.eventOffset()));
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
                .onErrorMap(failure -> handlerFailure(failure, confirmationTimeout))
                .map(results -> new HandlerDecision(results, null))
                .onErrorResume(AgentInvocationException.class, failure ->
                        Mono.just(new HandlerDecision(null, failure)))
                .flatMap(decision -> decision.failure() == null
                        ? continueWithResults(
                                call,
                                context,
                                request,
                                decision.results(),
                                failOnDeniedTool,
                                cleanupTimeout)
                        : cleanupThenFail(
                                call,
                                context,
                                request.toolCalls(),
                                decision.failure(),
                                cleanupTimeout));
    }

    private Mono<NormalRound> continueWithResults(
            AgentCall call,
            LiteFlowAgentContext context,
            ConfirmationRequest request,
            List<ConfirmResult> results,
            boolean failOnDeniedTool,
            Duration cleanupTimeout) {
        List<ConfirmResult> validated;
        try {
            validated = ConfirmationResultValidator.validateResults(request, results);
        } catch (RuntimeException failure) {
            return cleanupThenFail(
                    call,
                    context,
                    request.toolCalls(),
                    permissionFailure("Invalid HITL confirmation results", failure),
                    cleanupTimeout);
        }
        if (failOnDeniedTool
                && validated.stream().anyMatch(result -> !result.isConfirmed())) {
            return cleanupThenFail(
                    call,
                    context,
                    request.toolCalls(),
                    permissionFailure("A tool confirmation was denied", null),
                    cleanupTimeout);
        }
        int eventOffset = confirmationEventCount(context);
        Mono<Msg> continuation = awaitTermination(withRuntimeDeadline(
                call.invoke(resumeMessage(validated)), context));
        return continuation
                .onErrorResume(AgentInvocationException.class, failure ->
                        failure.getErrorType() == AgentInvocationErrorType.PERMISSION
                                || failure.getErrorType() == AgentInvocationErrorType.TIMEOUT
                                ? cleanupThenFail(
                                        call,
                                        context,
                                        request.toolCalls(),
                                        failure,
                                        cleanupTimeout)
                                : Mono.error(failure))
                .map(nextReply -> new NormalRound(nextReply, eventOffset));
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
            return Mono.error(marker);
        }
        return source.timeout(timeout, Mono.error(marker), timeoutScheduler)
                .doOnError(RuntimeDeadlineExceededException.class, failure ->
                        context.cancel())
                .doOnError(ConfirmationDeadlineExceededException.class, failure ->
                        context.cancel());
    }

    private static Throwable handlerFailure(
            Throwable failure, Duration confirmationTimeout) {
        if (failure instanceof RuntimeDeadlineExceededException) {
            return runtimeFailure(failure);
        }
        if (failure instanceof ConfirmationDeadlineExceededException) {
            return new AgentInvocationException(
                    AgentInvocationErrorType.TIMEOUT,
                    "Agent confirmation exceeded timeout " + confirmationTimeout,
                    failure);
        }
        return permissionFailure("AgentConfirmationHandler failed", failure);
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

    private <T> Mono<T> cleanupThenFail(
            AgentCall call,
            LiteFlowAgentContext context,
            List<ToolUseBlock> pending,
            AgentInvocationException intendedFailure,
            Duration cleanupTimeout) {
        Mono<Void> cleanup = Mono.just(new CleanupRound(pending, false))
                .expand(round -> round.complete()
                        ? Mono.empty()
                        : advanceCleanupRound(call, context, round.pending()))
                .filter(CleanupRound::complete)
                .next()
                .then()
                .timeout(
                        cleanupTimeout,
                        Mono.error(new CleanupDeadlineExceededException(cleanupTimeout)),
                        timeoutScheduler);
        return cleanup
                .then(Mono.<T>error(intendedFailure))
                .onErrorResume(cleanupFailure -> failAfterCleanup(
                        intendedFailure, cleanupFailure));
    }

    private Mono<CleanupRound> advanceCleanupRound(
            AgentCall call,
            LiteFlowAgentContext context,
            List<ToolUseBlock> pending) {
        List<ConfirmResult> denied = ConfirmationResultValidator.denyAll(pending);
        int eventOffset = confirmationEventCount(context);
        return awaitTermination(call.invoke(resumeMessage(denied)))
                .map(reply -> {
                    if (!isAsking(reply)) {
                        return new CleanupRound(List.of(), true);
                    }
                    ConfirmationRequest request = ConfirmationResultValidator.request(
                            reply, confirmationEventsSince(context, eventOffset));
                    return new CleanupRound(request.toolCalls(), false);
                });
    }

    private static <T> Mono<T> failAfterCleanup(
            AgentInvocationException intendedFailure, Throwable cleanupFailure) {
        if (cleanupFailure != intendedFailure) {
            intendedFailure.addSuppressed(cleanupFailure);
        }
        return Mono.error(intendedFailure);
    }

    private static boolean isAsking(Msg reply) {
        return reply.getGenerateReason() == GenerateReason.PERMISSION_ASKING;
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
            AgentCallTarget target,
            AgentOutputSpec output,
            RuntimeContext runtimeContext) {
        return switch (output.kind()) {
            case TEXT -> input -> target.call(input, runtimeContext);
            case JAVA_TYPE -> {
                Class<?> javaType = output.javaType();
                yield input -> target.call(input, javaType, runtimeContext);
            }
            case JSON_SCHEMA -> {
                com.fasterxml.jackson.databind.JsonNode jsonSchema = output.jsonSchema();
                yield input -> target.call(input, jsonSchema, runtimeContext);
            }
        };
    }

    private static List<Msg> resumeMessage(List<ConfirmResult> results) {
        return List.of(UserMessage.builder()
                .metadata(Map.of(Msg.METADATA_CONFIRM_RESULTS, List.copyOf(results)))
                .build());
    }

    private static int confirmationEventCount(LiteFlowAgentContext context) {
        return context.getConfirmationEvents().size();
    }

    private static List<RequireUserConfirmEvent> confirmationEventsSince(
            LiteFlowAgentContext context, int eventOffset) {
        List<RequireUserConfirmEvent> snapshot = context.getConfirmationEvents();
        if (eventOffset < 0 || eventOffset > snapshot.size()) {
            throw new IllegalArgumentException("Invalid confirmation event offset " + eventOffset);
        }
        return List.copyOf(snapshot.subList(eventOffset, snapshot.size()));
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

    private record HandlerDecision(
            List<ConfirmResult> results, AgentInvocationException failure) {
    }

    private record NormalRound(Msg reply, int eventOffset) {
    }

    private record CleanupRound(List<ToolUseBlock> pending, boolean complete) {
    }
}
