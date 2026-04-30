package com.yomahub.liteflow.agent.session;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.id.NanoId;
import cn.hutool.core.util.StrUtil;

import java.util.Date;

public class NanoIdSessionIdGenerator {

    private static final char[] CODE_ALPHABET =
            "123456789ABCDEFGHIJKLMNPQRSTUVWXYZ".toCharArray();

    public static String generate() {
        String date = DateUtil.format(new Date(), "yyyyMMdd");
        String code = NanoId.randomNanoId(null, CODE_ALPHABET, 12);
        return StrUtil.format("{}_{}", date, code);
    }
}
