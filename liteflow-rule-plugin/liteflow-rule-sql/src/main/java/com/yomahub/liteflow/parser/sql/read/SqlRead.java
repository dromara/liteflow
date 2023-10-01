package com.yomahub.liteflow.parser.sql.read;

import com.yomahub.liteflow.parser.constant.ReadType;

import java.util.Map;

/**
 * sql 读取接口
 *
 * @author tangkc
 * @author houxinyu
 * @since 2.11.1
 */
public interface SqlRead {

    /**
     * 读取
     *
     * @return 返回读取到的数据
     */
    Map<String/*规则唯一键*/, String/*规则内容*/> read();

    /**
     * 类型
     *
     * @return 返回类型
     */
    ReadType type();
}
