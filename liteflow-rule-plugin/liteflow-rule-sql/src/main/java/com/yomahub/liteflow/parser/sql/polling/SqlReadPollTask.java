package com.yomahub.liteflow.parser.sql.polling;

import com.yomahub.liteflow.parser.constant.ReadType;

import java.util.List;
import java.util.Map;

/**
 * sql 轮询任务接口
 *
 * @author tangkc
 * @author houxinyu
 * @since 2.11.1
 */
public interface SqlReadPollTask<T> {

    /**
     * 执行
     */
    void execute();

    /**
     * 初始化数据
     *
     */
    void initData(List<T> dataList);

    /**
     * 类型
     *
     * @return 类型
     */
    ReadType type();
}
