package com.yomahub.liteflow.parser.sql.polling;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.read.SqlRead;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * sql 轮询任务抽象类，维护公共方法
 *
 * @author tangkc
 * @author houxinyu
 * @author Bryan.Zhang
 * @since 2.11.1
 */
public abstract class AbstractSqlReadPollTask<T> implements SqlReadPollTask<T> {
    private final Map<String/*唯一键*/, String/*data-xml的sha1值*/> DATA_SHA_MAP = new HashMap<>();
    private final SqlRead<T> read;

    public AbstractSqlReadPollTask(SqlRead<T> read) {
        this.read = read;

        if (!read.type().equals(type())) {
            throw new ELSQLException("SqlReadPollTask type not match");
        }
    }

    @Override
    public void execute() {
        List<T> dataList = read.read();
        // 新增或者更新的元素
        List<T> saveElementList = new ArrayList<>();
        // 删除的元素
        List<String> deleteElementIds;

        for (T data : dataList) {
            String id = getKey(data);
            String newSHA = getNeedSha1Value(data);

            // 新增
            // 如果封装的SHAMap中不存在该chain, 表示该元素为新增
            if (!DATA_SHA_MAP.containsKey(id)) {
                saveElementList.add(data);

                DATA_SHA_MAP.put(id, newSHA);
            }
            // 修改
            // SHA值发生变化,表示该元素的值已被修改,重新拉取变化的chain
            else if (!StrUtil.equals(newSHA, DATA_SHA_MAP.get(id))) {
                saveElementList.add(data);

                DATA_SHA_MAP.put(id, newSHA);
            }
        }

        Set<String> oldIdList = DATA_SHA_MAP.keySet();  // 旧的 id 列表
        Set<String> newIdList = dataList.stream().map(this::getKey).collect(Collectors.toSet());       // 新的 id 列表
        // 计算单差集
        // 计算集合的单差集，即只返回【oldIdList】中有，但是【newIdList】中没有的元素，例如：
        //  subtractToList([1,2,3,4],[2,3,4,5]) -》 [1]
        deleteElementIds = CollUtil.subtractToList(oldIdList, newIdList);

        for (String id : deleteElementIds) {
            DATA_SHA_MAP.remove(id);
        }

        if (CollUtil.isNotEmpty(saveElementList)) {
            doSave(saveElementList);
        }

        if (CollUtil.isNotEmpty(deleteElementIds)) {
            doDelete(deleteElementIds);
        }
    }

    @Override
    public void initData(List<T> dataList) {
        DATA_SHA_MAP.putAll(shaValue(dataList));
    }

    public abstract void doSave(List<T> saveElementList);

    public abstract void doDelete(List<String> deleteElementId);

    private Map<String/*唯一键*/, String/*data-xml的sha1值*/> shaValue(List<T> dataList) {
        Map<String, String> result = new HashMap<>();
        dataList.forEach(t -> result.put(getKey(t), DigestUtil.sha1Hex(getNeedSha1Value(t))));
        return result;
    }

    private String getNeedSha1Value(T data) {
        if (StrUtil.isBlank(getExtValue(data))) {
            return DigestUtil.sha1Hex(getValue(data));
        }else{
            return DigestUtil.sha1Hex(getValue(data) + "|||" + getExtValue(data));
        }
    }

    // 如果是chain，那就是返回chain的id，如果是script，那就返回script的id
    protected abstract String getKey(T t);

    // 如果是chain，那就返回EL，如果是script，那就返回脚本数据
    protected abstract String getValue(T t);

    // 如果是chain，那就返回route el，如果是script，这个不返回，因为script没有扩展value
    protected abstract String getExtValue(T t);
}
