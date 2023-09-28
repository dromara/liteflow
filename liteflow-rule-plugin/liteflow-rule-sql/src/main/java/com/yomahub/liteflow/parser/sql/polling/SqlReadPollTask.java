package com.yomahub.liteflow.parser.sql.polling;

import com.yomahub.liteflow.parser.constant.ReadType;

/**
 * Copyright (C), 2021, 北京同创永益科技发展有限公司
 *
 * @author tangkc
 * @version 3.0.0
 * @description
 * @date 2023/9/28 14:10
 */
public interface SqlReadPollTask {

    void execute();

    ReadType type();
}
