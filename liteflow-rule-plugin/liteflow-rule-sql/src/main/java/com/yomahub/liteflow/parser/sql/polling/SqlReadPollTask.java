package com.yomahub.liteflow.parser.sql.polling;

import com.yomahub.liteflow.parser.constant.ReadType;

/**
 * sql 轮询任务接口
 *
 * @author tangkc huxinyu
 * @date 2023/9/28 11:49
 * @since 2.11.1
 */
public interface SqlReadPollTask {

    /**
     * 执行
     */
    void execute();

    /**
     * 类型
     */
    ReadType type();
}
