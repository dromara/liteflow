package com.yomahub.liteflow.parser.sql.read;

import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.sql.polling.SqlReadPollTask;
import com.yomahub.liteflow.parser.sql.polling.impl.ChainReadPollTask;
import com.yomahub.liteflow.parser.sql.polling.impl.ScriptReadPollTask;
import com.yomahub.liteflow.parser.sql.read.impl.ChainRead;
import com.yomahub.liteflow.parser.sql.read.impl.InstanceIdRead;
import com.yomahub.liteflow.parser.sql.read.impl.ScriptRead;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;

import java.util.HashMap;
import java.util.Map;

/**
 * sql 读取工厂类
 *
 * @author tangkc
 * @author houxinyu
 * @author Jay li
 * @since 2.11.1
 */
public class SqlReadFactory {
    private static final Map<ReadType, SqlRead<?>> READ_MAP = new HashMap<>();
    private static final Map<ReadType, SqlReadPollTask<?>> POLL_TASK_MAP = new HashMap<>();

    public static void registerRead(SQLParserVO config) {
        READ_MAP.put(ReadType.CHAIN, new ChainRead(config));
        READ_MAP.put(ReadType.SCRIPT, new ScriptRead(config));
        READ_MAP.put(ReadType.INSTANCE_ID, new InstanceIdRead(config));
    }

    public static void registerSqlReadPollTask(ReadType readType) {
        SqlRead<?> sqlRead = getSqlRead(readType);
        if (ReadType.CHAIN.equals(readType)) {
            POLL_TASK_MAP.put(ReadType.CHAIN, new ChainReadPollTask((ChainRead)sqlRead));
        } else if (ReadType.SCRIPT.equals(readType)) {
            POLL_TASK_MAP.put(ReadType.SCRIPT, new ScriptReadPollTask((ScriptRead)sqlRead));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> SqlRead<T> getSqlRead(ReadType readType) {
        return (SqlRead<T>)READ_MAP.get(readType);
    }

    @SuppressWarnings("unchecked")
    public static <T> SqlReadPollTask<T> getSqlReadPollTask(ReadType readType) {
        return (SqlReadPollTask<T>)POLL_TASK_MAP.get(readType);
    }
}
