package com.yomahub.liteflow.parser.redis.mode;

import cn.hutool.core.collection.CollectionUtil;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.event.MapEntryListener;
import org.redisson.client.codec.StringCodec;

import java.util.Arrays;
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
     * @param key hash name
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
     *
     * @param key hash name
     * @param listener listener
     * @return listener id
     */
    public int addListener(String key, MapEntryListener listener) {
        RMapCache<Object, Object> mapCache = redissonClient.getMapCache(key);
        return mapCache.addListener(listener);
    }

    /**
     * get all keys of hash
     *
     * @param key hash name
     * @return keySet
     */
    public Set<String> hkeys(String key) {
        RMap<String, String> map = redissonClient.getMap(key, StringCodec.INSTANCE);
        return map.readAllKeySet();
    }

    /**
     * gey value of the key
     *
     * @param key hash name
     * @param field hash field
     * @return hash value
     */
    public String hget(String key, String field) {
        RMap<String, String> map = redissonClient.getMap(key, StringCodec.INSTANCE);
        return map.get(field);
    }

    /**
     * Loads Lua script into Redis scripts cache and returns its SHA-1 digest
     * @param luaScript script
     * @return shaDigest
     */
    public String scriptLoad(String luaScript) {
        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        return script.scriptLoad(luaScript);
    }

    /**
     * Executes Lua script stored in Redis scripts cache by SHA-1 digest
     * @param shaDigest script cache by SHA-1
     * @param args script args
     * @return string
     */
    public String evalSha(String shaDigest, String... args){
        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        return  script.evalSha(RScript.Mode.READ_ONLY, shaDigest, RScript.ReturnType.VALUE,
                Arrays.asList(args)).toString();
    }
}
