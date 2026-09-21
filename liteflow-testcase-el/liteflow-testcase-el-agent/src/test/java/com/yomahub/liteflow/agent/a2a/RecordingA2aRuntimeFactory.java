package com.yomahub.liteflow.agent.a2a;

import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Exposes the production per-subscription A2A handle factory as an offline test seam. */
public final class RecordingA2aRuntimeFactory {

    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    private static final AtomicReference<Throwable> NEXT_FAILURE = new AtomicReference<>();
    private static final List<Object> CREATED_HANDLES = new CopyOnWriteArrayList<>();

    private RecordingA2aRuntimeFactory() {
    }

    public static A2aClientRuntimeFactory create() {
        return A2aClientRuntimeFactories.perCall(request -> {
            RecordingHandle handle = new RecordingHandle(NEXT_ID.incrementAndGet());
            CREATED_HANDLES.add(handle);
            return handle;
        });
    }

    public static List<Object> createdHandles() {
        return List.copyOf(CREATED_HANDLES);
    }

    public static void failNext(Throwable failure) {
        NEXT_FAILURE.set(failure);
    }

    public static void reset() {
        NEXT_ID.set(0);
        NEXT_FAILURE.set(null);
        CREATED_HANDLES.clear();
    }

    private static final class RecordingHandle implements A2aAgentHandle {

        private final int id;

        private RecordingHandle(int id) {
            this.id = id;
        }

        @Override
        public Mono<Msg> call(UserMessage message) {
            Throwable failure = NEXT_FAILURE.getAndSet(null);
            if (failure != null) {
                return Mono.error(failure);
            }
            return Mono.just(AssistantMessage.builder()
                    .textContent("remote-" + id)
                    .build());
        }

        @Override
        public void interrupt() {
        }
    }
}
