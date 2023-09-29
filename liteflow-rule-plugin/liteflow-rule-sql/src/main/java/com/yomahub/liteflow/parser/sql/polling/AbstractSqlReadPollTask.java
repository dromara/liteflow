package com.yomahub.liteflow.parser.sql.polling;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.read.SqlRead;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * sql 轮询任务抽象类，维护公共方法
 *
 * @author tangkc huxinyu
 * @date 2023/9/28 11:49
 * @since 2.11.1
 */
public abstract class AbstractSqlReadPollTask implements SqlReadPollTask {
    private final Map<String/*唯一键*/, String/*data-xml的sha1值*/> DATA_SHA_MAP;
    private final SqlRead read;

    public AbstractSqlReadPollTask(Map<String, String> dataMap, SqlRead read) {
        this.read = read;
        this.DATA_SHA_MAP = shaMapValue(dataMap);

        if (!read.type().equals(type())) {
            throw new ELSQLException("SqlReadPollTask type not match");
        }
    }

    @Override
    public void execute() {
        Map<String/*唯一键*/, String/*data-xml*/> newData = read.read();
        // 新增或者更新的元素
        Map<String, String> saveElementMap = new HashMap();
        // 删除的元素
        List<String> deleteElementIds = new ArrayList<>();

        for (Map.Entry<String, String> entry : newData.entrySet()) {
            String id = entry.getKey();
            String element = entry.getValue();
            String newSHA = DigestUtil.sha1Hex(element);

            // 新增
            // 如果封装的SHAMap中不存在该chain, 表示该元素为新增
            if (!DATA_SHA_MAP.containsKey(id)) {
                saveElementMap.put(id, element);

                DATA_SHA_MAP.put(id, newSHA);
            }
            // 修改
            // SHA值发生变化,表示该元素的值已被修改,重新拉取变化的chain
            else if (!StrUtil.equals(newSHA, DATA_SHA_MAP.get(id))) {
                saveElementMap.put(id, element);

                DATA_SHA_MAP.put(id, newSHA);
            }
        }

        Set<String> oldIdList = DATA_SHA_MAP.keySet();  // 旧的 id 列表
        Set<String> newIdList = newData.keySet();       // 新的 id 列表
        // 计算单差集
        // 计算集合的单差集，即只返回【oldIdList】中有，但是【newIdList】中没有的元素，例如：
        //  subtractToList([1,2,3,4],[2,3,4,5]) -》 [1]
        deleteElementIds = CollUtil.subtractToList(oldIdList, newIdList);

        for (String id : deleteElementIds) {
            DATA_SHA_MAP.remove(id);
        }

        if (CollUtil.isNotEmpty(saveElementMap)) {
            doSave(saveElementMap);
        }

        if (CollUtil.isNotEmpty(deleteElementIds)) {
            doDelete(deleteElementIds);
        }
    }

    public abstract void doSave(Map<String, String> saveElementMap);

    public abstract void doDelete(List<String> deleteElementId);

    private Map<String/*唯一键*/, String/*data-xml的sha1值*/> shaMapValue(Map<String, String> dataMap) {
        Map<String, String> result = new HashMap<>();
        dataMap.forEach((k, v) -> {
            result.put(k, DigestUtil.sha1Hex(v));
        });

        return result;
    }
}
