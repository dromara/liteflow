package com.yomahub.liteflow.parser.spi.instanceId;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.flow.entity.InstanceInfoDto;
import com.yomahub.liteflow.flow.instanceId.BaseNodeInstanceIdManageSpi;
import com.yomahub.liteflow.parser.constant.ReadType;
import com.yomahub.liteflow.parser.constant.SqlReadConstant;
import com.yomahub.liteflow.parser.sql.read.SqlRead;
import com.yomahub.liteflow.parser.sql.read.SqlReadFactory;
import com.yomahub.liteflow.parser.sql.read.vo.InstanceIdVO;
import com.yomahub.liteflow.parser.sql.util.JDBCHelper;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * sql方式生成instanceId
 *
 * @author Jay li
 * @since 2.12.4
 */

public class SqlNodeInstanceIdManageSpiImpl extends BaseNodeInstanceIdManageSpi {

    @Override
    public List<String> readInstanceIdFile(String chainId) {
        SqlRead<InstanceIdVO> insIdRead = SqlReadFactory.getSqlRead(ReadType.INSTANCE_ID);
        List<InstanceIdVO> readResult = insIdRead.read(chainId);

        if (CollectionUtil.isNotEmpty(readResult)) {
            return Arrays.asList(readResult.get(0).getElDataMd5(), readResult.get(0).getNodeInstanceIdMapJson());
        }

        return Collections.emptyList();
    }

    @Override
    public void writeInstanceIdFile(List<InstanceInfoDto> instanceIdList, String elMd5, String chainId) {
        JDBCHelper jdbcHelper = JDBCHelper.getInstance();
        SQLParserVO conf = jdbcHelper.getSqlParserVO();

        String insertSql = StrUtil.format(SqlReadConstant.INSTANT_INSERT_SQL, conf.getInstanceIdTableName(), conf.getInstanceIdApplicationNameField(),
                conf.getNodeInstanceIdMapJsonField(), conf.getElDataMd5Field(), conf.getInstanceChainIdField(), conf.getApplicationName(), JsonUtil.toJsonString(instanceIdList),
                elMd5, chainId);
        String updateSql = StrUtil.format(SqlReadConstant.INSTANT_UPDATE_SQL, conf.getInstanceIdTableName(), conf.getElDataMd5Field(), elMd5,
                conf.getNodeInstanceIdMapJsonField(), JsonUtil.toJsonString(instanceIdList), conf.getInstanceChainIdField(), chainId, conf.getInstanceIdApplicationNameField(), conf.getApplicationName());
        String selectSql = StrUtil.format(SqlReadConstant.INSTANT_SELECT_SQL, conf.getInstanceIdTableName(), conf.getInstanceChainIdField(), chainId,
                conf.getInstanceIdApplicationNameField(), conf.getApplicationName());

        jdbcHelper.executeUpsert(selectSql, insertSql, updateSql);
    }

}
