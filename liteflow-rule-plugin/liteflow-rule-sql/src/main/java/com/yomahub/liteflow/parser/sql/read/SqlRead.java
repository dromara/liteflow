package com.yomahub.liteflow.parser.sql.read;

import com.yomahub.liteflow.parser.constant.ReadType;

import java.util.Map;

/**
 * sql 读取接口
 *
 * @author tangkc huxinyu
 * @date 2023/9/28 11:49
 * @since 2.11.1
 */
public interface SqlRead {

    /**
     * 读取
     */
    Map<String/*规则唯一键*/, String/*规则内容*/> read();

    /**
     * 类型
     */
    ReadType type();
}
