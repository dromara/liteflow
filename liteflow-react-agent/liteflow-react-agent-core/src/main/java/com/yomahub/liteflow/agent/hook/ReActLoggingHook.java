package com.yomahub.liteflow.agent.hook;

import io.agentscope.core.hook.ErrorEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 把 agentscope ReActAgent 的内部 Reason / Act / Error 事件输出到日志，
 * 让 LiteFlow 用户在终端可以直接看到 agent 的思考与工具调用过程。
 *
 * <p>事件 → 日志格式：
 * <ul>
 *   <li>{@link PreReasoningEvent}：{@code [agent:reason] >>> model=... messages=N}</li>
 *   <li>{@link PostReasoningEvent}：{@code [agent:reason] <<< text=... toolCalls=[...]}</li>
 *   <li>{@link PreActingEvent}：{@code [agent:act] >>> tool=... input=...}</li>
 *   <li>{@link PostActingEvent}：{@code [agent:act] <<< tool=... result=...}</li>
 *   <li>{@link ErrorEvent}：{@code [agent:error] ...}</li>
 * </ul>
 */
public class ReActLoggingHook implements Hook {

    private static final Logger LOG = LoggerFactory.getLogger(ReActLoggingHook.class);
    private static final int MAX_TEXT_LEN = 500;

    private final String sessionId;

    public ReActLoggingHook(String sessionId) {
        this.sessionId = sessionId == null ? "-" : sessionId;
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        try {
            if (event instanceof PreReasoningEvent e) {
                List<Msg> msgs = e.getInputMessages();
                LOG.info("[agent:reason][{}] >>> model={} messages={}",
                        sessionId, e.getModelName(), msgs == null ? 0 : msgs.size());
            } else if (event instanceof PostReasoningEvent e) {
                Msg reply = e.getReasoningMessage();
                String text = reply == null ? "" : truncate(reply.getTextContent());
                List<ToolUseBlock> tools = reply == null
                        ? List.of()
                        : reply.getContentBlocks(ToolUseBlock.class);
                if (tools.isEmpty()) {
                    LOG.info("[agent:reason][{}] <<< text={}", sessionId, text);
                } else {
                    LOG.info("[agent:reason][{}] <<< text={} toolCalls={}",
                            sessionId, text, summarizeToolUses(tools));
                }
            } else if (event instanceof PreActingEvent e) {
                ToolUseBlock t = e.getToolUse();
                LOG.info("[agent:act][{}] >>> tool={} input={}",
                        sessionId, t.getName(), truncate(String.valueOf(t.getInput())));
            } else if (event instanceof PostActingEvent e) {
                ToolResultBlock r = e.getToolResult();
                LOG.info("[agent:act][{}] <<< tool={} result={}",
                        sessionId, r.getName(), truncate(blocksToString(r)));
            } else if (event instanceof ErrorEvent e) {
                LOG.warn("[agent:error][{}] {}", sessionId, e.getError().toString(), e.getError());
            }
        } catch (Throwable logEx) {
            LOG.debug("ReActLoggingHook formatting failed", logEx);
        }
        return Mono.just(event);
    }

    @Override
    public int priority() {
        return 900;
    }

    private static String summarizeToolUses(List<ToolUseBlock> tools) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tools.size(); i++) {
            ToolUseBlock t = tools.get(i);
            if (i > 0) sb.append(", ");
            sb.append(t.getName()).append("(").append(t.getInput()).append(")");
        }
        return truncate(sb.append("]").toString());
    }

    private static String blocksToString(ToolResultBlock r) {
        if (r.getOutput() == null) return "";
        StringBuilder sb = new StringBuilder();
        r.getOutput().forEach(b -> sb.append(b));
        return sb.toString();
    }

    private static String truncate(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() <= MAX_TEXT_LEN ? s : s.substring(0, MAX_TEXT_LEN) + "...(truncated)";
    }
}
