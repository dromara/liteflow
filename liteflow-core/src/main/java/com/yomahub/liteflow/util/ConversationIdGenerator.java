package com.yomahub.liteflow.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.id.NanoId;
import cn.hutool.core.util.StrUtil;

import java.util.Date;

/**
 * 默认的 {@code conversationId} 生成器：日期前缀 + 12 位 NanoId。
 *
 * <p>{@code conversationId} 用于在 chain 执行期间标识一段"业务会话"，由
 * {@link com.yomahub.liteflow.slot.Slot#setConversationId(String)} 写入 slot，
 * 同 chain 内所有需要会话上下文的组件（典型如 ReAct Agent）共享。
 *
 * <p>不传 conversationId 时由 {@link com.yomahub.liteflow.core.FlowExecutor} 在 slot
 * 创建后调用本工具生成一次性标识；传入则原样使用，便于跨调用恢复会话。
 */
public final class ConversationIdGenerator {

    private static final char[] CODE_ALPHABET =
            "123456789ABCDEFGHIJKLMNPQRSTUVWXYZ".toCharArray();

    private ConversationIdGenerator() {}

    public static String generate() {
        String date = DateUtil.format(new Date(), "yyyyMMdd");
        String code = NanoId.randomNanoId(null, CODE_ALPHABET, 12);
        return StrUtil.format("{}_{}", date, code);
    }
}
