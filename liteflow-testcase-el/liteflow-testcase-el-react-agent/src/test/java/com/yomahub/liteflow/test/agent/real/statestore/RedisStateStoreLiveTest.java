package com.yomahub.liteflow.test.agent.real.statestore;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

/**
 * guide §5.3 REDIS 状态存储（真实 Docker Redis）：
 * 多轮记忆 + key 落库 + strict-distributed 守卫校验。
 */
@TestPropertySource("classpath:/real/statestore-redis/application.properties")
@SpringBootTest(classes = RedisStateStoreLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.statestore")
public class RedisStateStoreLiveTest extends RealAgentTestBase {

    private static final String REDIS_URI = "redis://localhost:16379";
    private static final String KEY_PREFIX = "liteflow:real:test:";

    /** 两轮对话经真实 Redis 续接记忆。 */
    @Test
    public void multiTurnMemorySurvivesThroughRealRedis() {
        String cid = "real-redis-cid-" + UUID.randomUUID();
        LiteflowResponse first = flowExecutor.execute2Resp("realRedisChain",
                "请记住暗号 REDIS-DURIAN-9527，回复“好的”即可。",
                ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(first.isSuccess(), cause(first));

        LiteflowResponse second = flowExecutor.execute2Resp("realRedisChain",
                "我们这个会话的暗号是什么？只回复暗号本身。",
                ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(second.isSuccess(), cause(second));
        Assertions.assertTrue(reply(second).contains("REDIS-DURIAN-9527"),
                "memory must survive in redis, got: " + reply(second));
    }

    /** 会话状态真实写入 Redis（自定义 key 前缀可见）。 */
    @Test
    public void sessionStateIsStoredInRedisWithConfiguredPrefix() {
        String cid = "real-redis-key-" + UUID.randomUUID();
        LiteflowResponse response = flowExecutor.execute2Resp("realRedisChain",
                "请记住暗号 REDIS-KEY-3141，回复“好的”。",
                ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(response.isSuccess(), cause(response));

        RedisClient client = RedisClient.create(REDIS_URI);
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisCommands<String, String> sync = connection.sync();
            List<String> keys = sync.keys(KEY_PREFIX + "*");
            Assertions.assertFalse(keys.isEmpty(),
                    "session keys with prefix " + KEY_PREFIX + " must exist in redis");
            boolean stateKeyFound = keys.stream().anyMatch(key -> key.endsWith(":agent_state"));
            Assertions.assertTrue(stateKeyFound,
                    "an :agent_state key must exist, keys=" + keys);
        } finally {
            client.shutdown();
        }
    }

    /** §14.4 / §18：REDIS 共享存储 + 默认严格模式 + 本地守卫 → 构建期 fail-fast。 */
    @Test
    public void strictDistributedGuardRejectsLocalCoordinationForRedis() {
        boolean original = liteflowConfig.getAgent().getInvocationGuard().isStrictDistributed();
        liteflowConfig.getAgent().getInvocationGuard().setStrictDistributed(true);
        try {
            LiteflowResponse response = flowExecutor.execute2Resp("realRedisChain",
                    "你好", ExecuteOption.of().conversationId("real-redis-guard"));
            Assertions.assertFalse(response.isSuccess(), "strict guard must fail the chain");
            Assertions.assertTrue(cause(response).contains(
                            "may be distributed, but invocationGuard.coordinationMode=NONE"),
                    "unexpected cause: " + cause(response));
        } finally {
            liteflowConfig.getAgent().getInvocationGuard().setStrictDistributed(original);
        }
    }

    private static String reply(LiteflowResponse response) {
        Object data = response.getSlot().getResponseData();
        return data == null ? "" : String.valueOf(data);
    }

    private static String cause(LiteflowResponse response) {
        if (response.getCause() == null) {
            return "";
        }
        StringBuilder messages = new StringBuilder();
        for (Throwable current = response.getCause();
                current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(" <- ");
        }
        return messages.toString();
    }
}
