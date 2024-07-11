package com.yomahub.liteflow.parser.sql.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.constant.ReadType;

import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.polling.SqlReadPollTask;
import com.yomahub.liteflow.parser.sql.read.AbstractSqlRead;
import com.yomahub.liteflow.parser.sql.read.SqlRead;
import com.yomahub.liteflow.parser.sql.read.SqlReadFactory;
import com.yomahub.liteflow.parser.sql.read.vo.ChainVO;
import com.yomahub.liteflow.parser.sql.read.vo.ScriptVO;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.yomahub.liteflow.parser.constant.SqlReadConstant.*;

/**
 * jdbc 工具类
 *
 * @author tangkc
 * @since 2.9.0
 */
public class JDBCHelper {

    private SQLParserVO sqlParserVO;

    private static JDBCHelper INSTANCE;

    /**
     * 定时任务线程池核心线程数
     */
    private static final int CORE_POOL_SIZE = 2;

    /**
     * 定时任务线程池
     */
    private static ScheduledThreadPoolExecutor pollExecutor;

    private static LFLog LOG = LFLoggerManager.getLogger(JDBCHelper.class);

    /**
     * 初始化 INSTANCE
     */
    public static void init(SQLParserVO sqlParserVO) {
        try {
            INSTANCE = new JDBCHelper();
            if (StrUtil.isNotBlank(sqlParserVO.getDriverClassName())) {
                Class.forName(sqlParserVO.getDriverClassName());
            }
            INSTANCE.setSqlParserVO(sqlParserVO);
            // 创建定时任务线程池
            if (sqlParserVO.getPollingEnabled() && ObjectUtil.isNull(getPollExecutor())) {
                ThreadFactory namedThreadFactory = new NamedThreadFactory("SQL-Polling-", false);
                ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE, namedThreadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());
                setPollExecutor(threadPoolExecutor);
            }
        } catch (ClassNotFoundException e) {
            throw new ELSQLException(e);
        }
    }

    /**
     * 获取 INSTANCE
     *
     * @return 实例
     */
    public static JDBCHelper getInstance() {
        return INSTANCE;
    }

    /**
     * 获取 ElData 数据内容
     *
     * @return 数据内容
     */
    public String getContent() {
        SqlRead<ChainVO> chainRead = SqlReadFactory.getSqlRead(ReadType.CHAIN);
        SqlRead<ScriptVO> scriptRead = SqlReadFactory.getSqlRead(ReadType.SCRIPT);

        // 获取 chain 数据
        List<ChainVO> chainVOList = chainRead.read();
        List<String> chainList = new ArrayList<>();

        chainVOList.forEach(
                chainVO -> chainList.add(StrUtil.format(CHAIN_XML_PATTERN, XmlUtil.escape(chainVO.getChainId()), StrUtil.emptyIfNull(chainVO.getNamespace()), StrUtil.emptyIfNull(chainVO.getRoute()), chainVO.getBody()))
        );


        String chainsContent = CollUtil.join(chainList, StrUtil.EMPTY);

        // 获取脚本数据
        List<ScriptVO> scriptVOList = scriptRead.read();
        List<String> scriptList = new ArrayList<>();

        scriptVOList.forEach(scriptVO -> {
            String id = scriptVO.getNodeId();
            String name = scriptVO.getName();
            String type = scriptVO.getType();
            String language = scriptVO.getLanguage();
            String elData = scriptVO.getScript();

            if (StringUtils.isNotBlank(scriptVO.getLanguage())) {
                scriptList.add(StrUtil.format(NODE_ITEM_WITH_LANGUAGE_XML_PATTERN, XmlUtil.escape(id), XmlUtil.escape(name), type, language, elData));
            } else {
                scriptList.add(StrUtil.format(NODE_ITEM_XML_PATTERN, XmlUtil.escape(id), XmlUtil.escape(name), type, elData));
            }
        });

        String nodesContent = StrUtil.format(NODE_XML_PATTERN, CollUtil.join(scriptList, StrUtil.EMPTY));

        // 初始化轮询任务
        SqlReadPollTask<ChainVO> sqlReadPollTask4Chain = SqlReadFactory.getSqlReadPollTask(ReadType.CHAIN);
        sqlReadPollTask4Chain.initData(chainVOList);
        SqlReadPollTask<ScriptVO> sqlReadPollTask4Script = SqlReadFactory.getSqlReadPollTask(ReadType.SCRIPT);
        sqlReadPollTask4Script.initData(scriptVOList);
        return StrUtil.format(XML_PATTERN, nodesContent, chainsContent);
    }

    /**
     * 定时轮询拉取SQL中变化的数据
     */
    public void listenSQL() {
        // 添加轮询chain的定时任务
        pollExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        SqlReadFactory.getSqlReadPollTask(ReadType.SCRIPT).execute();
                        SqlReadFactory.getSqlReadPollTask(ReadType.CHAIN).execute();
                    } catch (Exception ex) {
                        LOG.error("poll chain fail", ex);
                    }
                },
                sqlParserVO.getPollingStartSeconds().longValue(),
                sqlParserVO.getPollingIntervalSeconds().longValue(),
                TimeUnit.SECONDS
        );
    }

    private void setSqlParserVO(SQLParserVO sqlParserVO) {
        this.sqlParserVO = sqlParserVO;
    }

    public static ScheduledThreadPoolExecutor getPollExecutor() {
        return pollExecutor;
    }

    public static void setPollExecutor(ScheduledThreadPoolExecutor pollExecutor) {
        JDBCHelper.pollExecutor = pollExecutor;
    }
}
