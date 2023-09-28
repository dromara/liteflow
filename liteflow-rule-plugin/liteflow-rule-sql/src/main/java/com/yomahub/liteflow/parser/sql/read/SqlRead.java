package com.yomahub.liteflow.parser.sql.read;

import com.yomahub.liteflow.parser.constant.ReadType;

import java.util.Map;

/**
 * Copyright (C), 2021, 北京同创永益科技发展有限公司
 *
 * @author tangkc
 * @version 3.0.0
 * @description
 * @date 2023/9/28 11:25
 */
public interface SqlRead {

    Map<String/*规则唯一键*/, String/*规则内容*/> read();

    ReadType type();
}
