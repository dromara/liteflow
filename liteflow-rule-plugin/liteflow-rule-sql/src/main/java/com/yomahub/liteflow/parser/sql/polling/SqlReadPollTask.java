package com.yomahub.liteflow.parser.sql.polling;

import com.yomahub.liteflow.parser.constant.ReadType;

import java.util.Map;

/**
 * sql 轮询任务接口
 *
 * @author tangkc
 * @author houxinyu
 * @since 2.11.1
 */
public interface SqlReadPollTask {

    /**
     * 执行
     */
    void execute();

    /**
     * 初始化数据
     *
     * @param dataMap 数据
     */
    void initData(Map<String/*唯一键*/, String/*data-xml的数据*/> dataMap);

    /**
     * 类型
     *
     * @return 类型
     */
    ReadType type();
}
