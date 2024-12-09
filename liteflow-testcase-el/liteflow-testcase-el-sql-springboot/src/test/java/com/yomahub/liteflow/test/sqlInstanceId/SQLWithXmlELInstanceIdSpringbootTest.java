package com.yomahub.liteflow.test.sqlInstanceId;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.entity.InstanceInfoDto;
import com.yomahub.liteflow.flow.instanceId.NodeInstanceIdManageSpi;
import com.yomahub.liteflow.flow.instanceId.NodeInstanceIdManageSpiHolder;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.assertj.core.util.Sets;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author jay li
 * @since 2.12.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/application-instanceId-xml.properties")
@SpringBootTest(classes = SQLWithXmlELInstanceIdSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.sql.cmp"})
public class SQLWithXmlELInstanceIdSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testSQLWithXmlChain() throws SQLException, JSONException {
        // 查询数据库实例id
        String instanceId = queryInstanceIdInfo("r_chain4");
        // 解析 JSON
        List<InstanceInfoDto> instanceInfos = JsonUtil.parseList(instanceId, InstanceInfoDto.class);
        // 构造实例id字符串
        StringBuilder result = new StringBuilder();
        int i = 0;

        for (InstanceInfoDto dto : instanceInfos) {
            result.append(dto.getNodeId()).append("[").append(dto.getInstanceId()).append("]");
            if (i + 1 < instanceInfos.size()) {
                result.append("==>");
            }
            i++;
        }

        LiteflowResponse response = flowExecutor.execute2Resp("r_chain4", "arg");
        Assertions.assertEquals("c==>b==>a", response.getExecuteStepStr());
        Assertions.assertEquals(result.toString(), response.getExecuteStepStrWithInstanceId());
        // 重复执行 检查实例id是否变化
        response = flowExecutor.execute2Resp("r_chain4", "arg");
        Assertions.assertEquals(result.toString(), response.getExecuteStepStrWithInstanceId());
    }


    // 测试sql实例id 构建 坐标返回
    @Test
    public void testSQLWithXmlChain2() {
        LiteflowResponse response = flowExecutor.execute2Resp("r_chain4", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("c==>b==>a", response.getExecuteStepStr());

        String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
        List<String> strings = extractValuesList(executeStepStrWithInstanceId);
        NodeInstanceIdManageSpi nodeInstanceIdManageSpi = NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi();

        String[] nodes = new String[]{"c", "b", "a"};
        for (int i = 0; i < strings.size(); i++) {
            Assertions.assertEquals(nodeInstanceIdManageSpi.getNodeInstanceLocationById("r_chain4", strings.get(i)), nodes[i] + "(0)");
        }

        HashSet<String> hashSet = Sets.newHashSet(strings);
        Assertions.assertEquals(hashSet.size(), 3);
    }

    // 测试chain 表达式更改后，实例id是否变化
    @Test
    public void testSQLWithXmlChain3() throws SQLException, JSONException {
        String chain4InstanceStr = querySqlInstanceId("r_chain4");
        LiteflowResponse response = flowExecutor.execute2Resp("r_chain4", "arg");
        Assertions.assertEquals("c==>b==>a", response.getExecuteStepStr());
        Assertions.assertEquals(chain4InstanceStr, response.getExecuteStepStrWithInstanceId());

        // 更该数据 查实例id是否变化
        changeData();
        flowExecutor.reloadRule();

        // 重复查询
        response = flowExecutor.execute2Resp("r_chain4", "arg");
        String chain4InstanceStr2 = querySqlInstanceId("r_chain4");
        Assertions.assertNotEquals(chain4InstanceStr2, chain4InstanceStr);
        Assertions.assertEquals("a==>c==>b", flowExecutor.execute2Resp("r_chain4", "arg").getExecuteStepStr());
        Assertions.assertEquals(chain4InstanceStr2, response.getExecuteStepStrWithInstanceId());
    }

    // 修改数据库数据
    private void changeData() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        SQLParserVO sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
        Connection connection;
        try {
            connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(),
                    sqlParserVO.getPassword());
            Statement statement = connection.createStatement();
            statement.executeUpdate("UPDATE EL_TABLE SET EL_DATA='THEN(a, c, b);' WHERE chain_name='r_chain4'");
        }
        catch (SQLException e) {
            throw new ELSQLException(e.getMessage());
        }
    }

    private String querySqlInstanceId(String chainId) throws SQLException {
        // 查询数据库实例id
        String instanceId = queryInstanceIdInfo(chainId);
        // 解析 JSON
        List<InstanceInfoDto> instanceInfoDtos = JsonUtil.parseList(instanceId, InstanceInfoDto.class);
        // 构造实例id字符串
        StringBuilder result = new StringBuilder();
        int i = 0;

        for (InstanceInfoDto dto : instanceInfoDtos) {
            result.append(dto.getNodeId()).append("[").append(dto.getInstanceId()).append("]");
            if (i + 1 < instanceInfoDtos.size()) {
                result.append("==>");
            }
            i++;
        }

        return result.toString();

    }

    public static List<String> extractValuesList(String input) {
        List<String> values = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[(.*?)]");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    public String queryInstanceIdInfo(String chainId) throws SQLException {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        SQLParserVO sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
        Connection connection;
        try {
            connection = DriverManager.getConnection(sqlParserVO.getUrl(), sqlParserVO.getUsername(),
                    sqlParserVO.getPassword());
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("select * from NODE_INSTANCE_ID_TABLE where APPLICATION_NAME = 'demo' " +
                    "and chain_id = '" + chainId + "' ");

            String res = "";
            while (rs.next()) {
                res = rs.getString("node_instance_id_map_json");
            }
            return res;
        } catch (SQLException e) {
            throw new ELSQLException(e.getMessage());
        }
    }
}
