package com.yomahub.liteflow.parser.sql.read;

import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.sql.polling.SqlReadPollTask;
import com.yomahub.liteflow.parser.sql.polling.impl.ChainReadPollTask;
import com.yomahub.liteflow.parser.sql.polling.impl.ScriptReadPollTask;
import com.yomahub.liteflow.parser.sql.read.impl.ChainRead;
import com.yomahub.liteflow.parser.sql.read.impl.ScriptRead;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (C), 2021, 北京同创永益科技发展有限公司
 *
 * @author tangkc
 * @version 3.0.0
 * @description
 * @date 2023/9/28 15:42
 */
public class SqlReadFactory {
    private static final Map<ReadType, SqlRead> READ_MAP = new HashMap<>();
    private static final Map<ReadType, SqlReadPollTask> POLL_TASK_MAP = new HashMap<>();

    public static void registerRead(SQLParserVO config) {
        READ_MAP.put(ReadType.CHAIN, new ChainRead(config));
        READ_MAP.put(ReadType.SCRIPT, new ScriptRead(config));
    }

    public static void registerSqlReadPollTask(ReadType readType, Map<String, String> dataMap) {
        SqlRead sqlRead = getSqlRead(readType);
        if (ReadType.CHAIN.equals(readType)) {
            POLL_TASK_MAP.put(ReadType.CHAIN, new ChainReadPollTask(dataMap, sqlRead));
        } else if (ReadType.SCRIPT.equals(readType)) {
            POLL_TASK_MAP.put(ReadType.SCRIPT, new ScriptReadPollTask(dataMap, sqlRead));
        }

    }

    public static SqlRead getSqlRead(ReadType readType) {
        return READ_MAP.get(readType);
    }

    public static SqlReadPollTask getSqlReadPollTask(ReadType readType) {
        return POLL_TASK_MAP.get(readType);
    }
}
