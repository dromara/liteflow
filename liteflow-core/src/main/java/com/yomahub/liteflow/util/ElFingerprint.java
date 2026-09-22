package com.yomahub.liteflow.util;

import cn.hutool.crypto.digest.MD5;

import java.nio.charset.StandardCharsets;

/**
 * EL 原文的统一指纹入口。
 * 保留空白、引号和分号等全部内容，只有原文一致的 EL 才复用 chain。
 */
public final class ElFingerprint {

    private ElFingerprint() {
    }

    public static String of(String el) {
        return MD5.create().digestHex(el, StandardCharsets.UTF_8);
    }
}
