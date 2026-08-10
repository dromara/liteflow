package com.yomahub.liteflow.agent.hitl;

import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ToolUseBlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Strict validation beyond AgentScope's subset-accepting confirmation contract. */
public final class ConfirmationResultValidator {

    private ConfirmationResultValidator() {
    }

    public static List<ToolUseBlock> pendingTools(Msg reply) {
        if (reply == null) {
            return List.of();
        }
        return reply.getContentBlocks(ToolUseBlock.class).stream()
                .filter(tool -> tool.getState() == ToolCallState.ASKING)
                .toList();
    }

    public static ConfirmationRequest request(
            Msg reply, List<RequireUserConfirmEvent> recordedEvents) {
        Objects.requireNonNull(reply, "reply");
        List<ToolUseBlock> replyTools = pendingTools(reply);
        Map<String, ToolUseBlock> replyById = indexed(replyTools, "reply");
        if (replyById.isEmpty()) {
            throw new IllegalArgumentException(
                    "PERMISSION_ASKING reply must contain ASKING ToolUseBlocks");
        }
        if (recordedEvents == null || recordedEvents.size() != 1) {
            throw new IllegalArgumentException(
                    "Exactly one RequireUserConfirmEvent is required");
        }
        RequireUserConfirmEvent event = recordedEvents.get(0);
        String replyId = textMetadata(reply, Msg.METADATA_CONFIRM_REQUEST_REPLY_ID);
        if (!replyId.equals(requireText(event.getReplyId(), "event replyId"))) {
            throw new IllegalArgumentException("Confirmation replyId does not match the event");
        }
        Map<String, ToolUseBlock> eventById = indexed(event.getToolCalls(), "event");
        if (!replyById.keySet().equals(eventById.keySet())) {
            throw new IllegalArgumentException("Event and reply tool IDs do not match");
        }
        for (Map.Entry<String, ToolUseBlock> entry : replyById.entrySet()) {
            ToolUseBlock fromReply = entry.getValue();
            ToolUseBlock fromEvent = eventById.get(entry.getKey());
            if (!sameTool(fromReply, fromEvent)) {
                throw new IllegalArgumentException(
                        "Event and reply tool data do not match for ID " + entry.getKey());
            }
        }
        return new ConfirmationRequest(replyId, event, replyTools);
    }

    public static List<ConfirmResult> validateResults(
            ConfirmationRequest request, List<ConfirmResult> results) {
        Objects.requireNonNull(request, "request");
        if (results == null) {
            throw new IllegalArgumentException("Confirmation results must not be null");
        }
        Map<String, ToolUseBlock> expected = indexed(request.toolCalls(), "request");
        Map<String, ConfirmResult> provided = new LinkedHashMap<>();
        for (ConfirmResult result : results) {
            if (result == null || result.getToolCall() == null) {
                throw new IllegalArgumentException(
                        "ConfirmResult and toolCall must not be null");
            }
            ToolUseBlock tool = result.getToolCall();
            String id = requireText(tool.getId(), "result tool ID");
            if (provided.putIfAbsent(id, result) != null) {
                throw new IllegalArgumentException("Duplicate confirmation result ID " + id);
            }
            ToolUseBlock original = expected.get(id);
            if (original == null) {
                throw new IllegalArgumentException("Unknown confirmation result ID " + id);
            }
            if (!Objects.equals(original.getName(), tool.getName())) {
                throw new IllegalArgumentException(
                        "Confirmation result must retain the original tool name for ID " + id);
            }
        }
        if (!provided.keySet().equals(expected.keySet())) {
            throw new IllegalArgumentException(
                    "Confirmation results must cover every pending tool exactly once");
        }
        return List.copyOf(results);
    }

    public static List<ConfirmResult> denyAll(List<ToolUseBlock> tools) {
        List<ConfirmResult> denied = new ArrayList<>();
        for (ToolUseBlock tool : tools == null ? List.<ToolUseBlock>of() : tools) {
            if (tool != null) {
                denied.add(new ConfirmResult(false, tool));
            }
        }
        return List.copyOf(denied);
    }

    private static Map<String, ToolUseBlock> indexed(
            List<ToolUseBlock> tools, String source) {
        Map<String, ToolUseBlock> indexed = new LinkedHashMap<>();
        if (tools == null) {
            return indexed;
        }
        for (ToolUseBlock tool : tools) {
            if (tool == null) {
                throw new IllegalArgumentException(source + " tool must not be null");
            }
            String id = requireText(tool.getId(), source + " tool ID");
            requireText(tool.getName(), source + " tool name");
            if (indexed.putIfAbsent(id, tool) != null) {
                throw new IllegalArgumentException(source + " contains duplicate tool ID " + id);
            }
        }
        return indexed;
    }

    private static String textMetadata(Msg reply, String key) {
        Object value = reply.getMetadata().get(key);
        return value instanceof String text
                ? requireText(text, "reply confirmation replyId")
                : requireText(null, "reply confirmation replyId");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static boolean sameTool(ToolUseBlock left, ToolUseBlock right) {
        return Objects.equals(left.getId(), right.getId())
                && Objects.equals(left.getName(), right.getName())
                && Objects.equals(left.getInput(), right.getInput())
                && Objects.equals(left.getContent(), right.getContent())
                && Objects.equals(left.getMetadata(), right.getMetadata())
                && left.getState() == right.getState();
    }
}
