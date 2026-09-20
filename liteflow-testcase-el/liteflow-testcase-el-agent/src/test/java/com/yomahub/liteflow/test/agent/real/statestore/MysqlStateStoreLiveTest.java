package com.yomahub.liteflow.test.agent.real.statestore;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

/**
 * guide §5.3 MYSQL 状态存储（真实 Docker MySQL 8）：
 * 自动建表 + 多轮记忆 + 表数据可查。
 */
@TestPropertySource("classpath:/real/statestore-mysql/application.properties")
@SpringBootTest(classes = MysqlStateStoreLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.statestore")
public class MysqlStateStoreLiveTest extends RealAgentTestBase {

    private static final String JDBC = "jdbc:mysql://localhost:13306/liteflow"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "root123456";

    /** 两轮对话经真实 MySQL 续接记忆。 */
    @Test
    public void multiTurnMemorySurvivesThroughRealMysql() {
        String cid = "real-mysql-cid-" + UUID.randomUUID();
        LiteflowResponse first = flowExecutor.execute2Resp("realMysqlChain",
                "请记住暗号 MYSQL-MANGO-15926，回复“好的”即可。",
                ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(first.isSuccess(), cause(first));

        LiteflowResponse second = flowExecutor.execute2Resp("realMysqlChain",
                "我们这个会话的暗号是什么？只回复暗号本身。",
                ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(second.isSuccess(), cause(second));
        Assertions.assertTrue(reply(second).contains("MYSQL-MANGO-15926"),
                "memory must survive in mysql, got: " + reply(second));
    }

    /** 会话状态真实写入 agentscope_sessions 表。 */
    @Test
    public void sessionRowsAreStoredInMysqlTable() throws Exception {
        String cid = "real-mysql-row-" + UUID.randomUUID();
        LiteflowResponse response = flowExecutor.execute2Resp("realMysqlChain",
                "请记住暗号 MYSQL-ROW-27182，回复“好的”。",
                ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(response.isSuccess(), cause(response));

        Assumptions.assumeTrue(mysqlReachable(), "mysql unreachable; skip row inspection");
        try (Connection connection = DriverManager.getConnection(JDBC, USER, PASSWORD);
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT COUNT(*) AS total FROM agent_state_real")) {
            Assertions.assertTrue(rows.next(), "count query must return a row");
            Assertions.assertTrue(rows.getInt("total") > 0,
                    "agent_state_real table must contain session rows");
        }
    }

    private static boolean mysqlReachable() {
        try (Connection ignored = DriverManager.getConnection(JDBC, USER, PASSWORD)) {
            return true;
        } catch (Exception e) {
            return false;
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
