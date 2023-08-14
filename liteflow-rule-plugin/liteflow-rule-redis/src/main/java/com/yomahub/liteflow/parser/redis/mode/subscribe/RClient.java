package com.yomahub.liteflow.parser.redis.mode.subscribe;

import cn.hutool.core.collection.CollectionUtil;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.event.MapEntryListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Redisson 客户端封装类.
 *
 * @author hxinyu
 * @since 2.11.0
 */
public class RClient {

    private final RedissonClient redissonClient;

    private Map<String, String> map = new HashMap<>();

    public RClient(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }


    /**
     * get hashmap of the key
     *
     * @param key
     * @return hashmap
     */
    public Map<String, String> getMap(String key) {
        RMapCache<String, String> mapCache = redissonClient.getMapCache(key);
        Set<String> mapFieldSet = mapCache.keySet();
        if (CollectionUtil.isEmpty(mapFieldSet)) {
            return map;
        }
        for (String field : mapFieldSet) {
            String value = mapCache.get(field);
            map.put(field, value);
        }
        return map;
    }


    /**
     * add listener of the key
     * @param key
     * @param listener
     * @return listener id
     */
    public int addListener(String key, MapEntryListener listener) {
        RMapCache<Object, Object> mapCache = redissonClient.getMapCache(key);
        return mapCache.addListener(listener);
    }
}
