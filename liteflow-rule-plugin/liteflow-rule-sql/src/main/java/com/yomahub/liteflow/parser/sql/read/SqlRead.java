package com.yomahub.liteflow.parser.sql.read;

import com.yomahub.liteflow.parser.constant.ReadType;

import java.util.List;

/**
 * sql 读取接口
 *
 * @author tangkc
 * @author houxinyu
 * @author Jay li
 * @since 2.11.1
 */
public interface SqlRead<T> {

    /**
     * 读取
     *
     * @return 返回读取到的数据
     */
    List<T> read();


    /**
     * 根据chainId 读取
     *
     * @return 返回读取到的数据
     */
    List<T> read(String chainId);


    /**
     * 类型
     *
     * @return 返回类型
     */
    ReadType type();
}
