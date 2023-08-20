package com.yomahub.liteflow.flow.element;


/**
 * 回滚接口 目前实现这个接口的只有Node
 *
 * @author RainZs
 */
public interface Rollbackable {

    void rollback(Integer slotIndex) throws Exception;

}
