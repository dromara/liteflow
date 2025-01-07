package com.yomahub.liteflow.parser.sql;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.sql.datasource.LiteflowDataSourceConnectFactory;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.read.SqlReadFactory;
import com.yomahub.liteflow.parser.sql.util.JDBCHelper;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Objects;

/**
 * SQL 解析器实现，只支持 EL 形式的 XML，不支持其他的形式
 *
 * @author tangkc
 * @since 2.9.0
 */
public class SQLXmlELParser extends ClassXmlFlowELParser {

    private static SQLParserVO sqlParserVO;

    private static final String ERROR_MSG_PATTERN = "rule-source-ext-data {} is blank";

    private static final String ERROR_COMMON_MSG = "rule-source-ext-data is empty";

    /**
     * 构造函数
     */
    public SQLXmlELParser() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

        try {
            if (MapUtil.isNotEmpty((liteflowConfig.getRuleSourceExtDataMap()))) {
                sqlParserVO = BeanUtil.toBean(liteflowConfig.getRuleSourceExtDataMap(), SQLParserVO.class,
                        CopyOptions.create());
            } else if (StrUtil.isNotBlank(liteflowConfig.getRuleSourceExtData())) {
                sqlParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), SQLParserVO.class);
            }
            if (Objects.isNull(sqlParserVO)) {
                throw new ELSQLException(ERROR_COMMON_MSG);
            }

            // 检查配置文件
            checkParserVO(sqlParserVO);

            // 初始化 JDBCHelper
            JDBCHelper.init(sqlParserVO);

            // 初始化 SqlReadFactory
            SqlReadFactory.registerRead(sqlParserVO);

            // 初始化连接器
            LiteflowDataSourceConnectFactory.register();

            // 注册轮询任务
            SqlReadFactory.registerSqlReadPollTask(ReadType.CHAIN);
            SqlReadFactory.registerSqlReadPollTask(ReadType.SCRIPT);


            // enable-node-instance-id=true 创建节点实例ID表 如果不存在
            if (liteflowConfig.getEnableNodeInstanceId()) {
                JDBCHelper.getInstance().createNodeInstanceIdTable();
            }
        } catch (ELSQLException elsqlException) {
            throw elsqlException;
        } catch (Exception ex) {
            throw new ELSQLException(ex);
        }

    }

    @Override
    public String parseCustom() {
        try {
            JDBCHelper jdbcHelper = JDBCHelper.getInstance();
            String content = jdbcHelper.getContent();
            if (sqlParserVO.getPollingEnabled()) {
                FlowInitHook.addHook(() -> {
                    jdbcHelper.listenSQL();
                    return true;
                });
            }
            return content;
        } catch (Exception ex) {
            throw new ELSQLException(ex);
        }
    }

    /**
     * 检查配置文件并设置默认值
     *
     * @param sqlParserVO sqlParserVO
     */
    private void checkParserVO(SQLParserVO sqlParserVO) {
        if (sqlParserVO.isAutoFoundDataSource()) {
            return;
        }
        if (StrUtil.isEmpty(sqlParserVO.getUrl())) {
            throw new ELSQLException(StrFormatter.format(ERROR_MSG_PATTERN, "url"));
        }
        if (StrUtil.isEmpty(sqlParserVO.getDriverClassName())) {
            throw new ELSQLException(StrFormatter.format(ERROR_MSG_PATTERN, "driverClassName"));
        }
        if (Objects.isNull(sqlParserVO.getUsername())) {
            throw new ELSQLException(StrFormatter.format(ERROR_MSG_PATTERN, "username"));
        }
        if (Objects.isNull(sqlParserVO.getPassword())) {
            throw new ELSQLException(StrFormatter.format(ERROR_MSG_PATTERN, "password"));
        }
    }

}
