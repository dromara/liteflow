package com.yomahub.liteflow.agent.redis.storage;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentSessionStoreRedisConfig;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisStringCommands;
import io.lettuce.core.api.sync.RedisScriptingCommands;
import org.redisson.api.RedissonClient;
import org.redisson.api.RScript;
import org.redisson.client.codec.StringCodec;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.ScanParams;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/** Owns connections it opens; never closes an application-owned Redis client. */
public final class RedisStorageConnection implements AutoCloseable {
    @FunctionalInterface public interface Script { long run(String script, List<String> keys, List<String> args); }
    public final Function<String,String> get;
    public final Consumer<String> delete;
    public final Function<String,Set<String>> keys;
    public final Script eval;
    private final AutoCloseable cleanup;
    private final java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();

    private RedisStorageConnection(Function<String,String> get, Consumer<String> delete,
            Function<String,Set<String>> keys, Script eval, AutoCloseable cleanup) {
        this.get=get; this.delete=delete; this.keys=keys; this.eval=eval; this.cleanup=cleanup;
    }
    public static RedisStorageConnection open(AgentSessionStoreRedisConfig config) {
        String uri=config.getUri(); String bean=config.getClientBeanName();
        if (uri != null && !uri.isBlank()) {
            if (bean != null && !bean.isBlank()) throw new AgentConfigException("Redis URI and bean are mutually exclusive");
            RedisClient client=RedisClient.create(uri);
            try {
                var connection=client.connect();
                return lettuce(connection.sync(),connection.sync(),connection.sync(),()->{connection.close();client.shutdown();});
            } catch (RuntimeException failure) {client.shutdown();throw failure;}
        }
        if (bean == null || bean.isBlank()) throw new AgentConfigException("Redis storage requires uri or client-bean-name");
        Object client=ContextAwareHolder.loadContextAware().getBean(bean);
        if (client instanceof RedisClient lettuce) {
            var connection=lettuce.connect();
            return lettuce(connection.sync(),connection.sync(),connection.sync(),connection::close);
        }
        if (client instanceof RedisClusterClient cluster) {
            var connection=cluster.connect();
            return lettuce(connection.sync(),connection.sync(),connection.sync(),connection::close);
        }
        if (client instanceof UnifiedJedis jedis) {
            return new RedisStorageConnection(jedis::get,key->jedis.del(key),pattern->{
                Set<String> keys=new HashSet<>();String cursor="0";
                do {var batch=jedis.scan(cursor,new ScanParams().match(pattern).count(500));keys.addAll(batch.getResult());cursor=batch.getCursor();} while(!"0".equals(cursor));
                return keys;
            },(script,keys,args)->((Number)jedis.eval(script,keys,args)).longValue(),()->{});
        }
        if (client instanceof RedissonClient redisson) {
            return new RedisStorageConnection(key->redisson.<String>getBucket(key,StringCodec.INSTANCE).get(),
                key->redisson.getBucket(key,StringCodec.INSTANCE).delete(),pattern->{
                    Set<String> keys=new HashSet<>();redisson.getKeys().getKeysByPattern(pattern).forEach(keys::add);return keys;
                },(script,keys,args)->((Number)redisson.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE,
                    script,RScript.ReturnType.INTEGER,new ArrayList<Object>(keys),args.toArray())).longValue(),()->{});
        }
        throw new AgentConfigException("Unified Redis storage requires a Jedis, Lettuce or Redisson client with Lua support");
    }
    private static RedisStorageConnection lettuce(RedisKeyCommands<String,String> keys,
            RedisStringCommands<String,String> strings, RedisScriptingCommands<String,String> scripts, AutoCloseable close) {
        return new RedisStorageConnection(strings::get,key->keys.del(key),pattern->{
            Set<String> found=new HashSet<>(); ScanCursor cursor=ScanCursor.INITIAL;
            do {var batch=keys.scan(cursor,ScanArgs.Builder.matches(pattern).limit(500));found.addAll(batch.getKeys());cursor=batch;} while(!cursor.isFinished());
            return found;
        },(script,k,args)->((Number)scripts.eval(script,ScriptOutputType.INTEGER,k.toArray(String[]::new),args.toArray(String[]::new))).longValue(),close);
    }
    public static String prefix(AgentSessionStoreRedisConfig config) {
        return config.getKeyPrefix()==null || config.getKeyPrefix().isBlank() ? "agentscope:session:" : config.getKeyPrefix();
    }
    @Override public void close() {if (!closed.compareAndSet(false, true)) return; try {cleanup.close();}catch(Exception failure){throw new IllegalStateException("Cannot close Redis connection",failure);}}
}
