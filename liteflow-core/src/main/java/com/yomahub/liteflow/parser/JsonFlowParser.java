package com.yomahub.liteflow.parser;

import com.alibaba.fastjson.JSONObject;
import com.yomahub.liteflow.builder.LiteFlowChainBuilder;
import com.yomahub.liteflow.builder.prop.ChainPropBean;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.parser.base.BaseJsonFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;

import static com.yomahub.liteflow.common.ChainConstant.ANY;
import static com.yomahub.liteflow.common.ChainConstant.CONDITION;
import static com.yomahub.liteflow.common.ChainConstant.ERROR_RESUME;
import static com.yomahub.liteflow.common.ChainConstant.GROUP;
import static com.yomahub.liteflow.common.ChainConstant.NAME;
import static com.yomahub.liteflow.common.ChainConstant.THREAD_EXECUTOR_CLASS;
import static com.yomahub.liteflow.common.ChainConstant.TYPE;
import static com.yomahub.liteflow.common.ChainConstant.VALUE;

/**
 * Json格式解析器
 *
 * @author guodongqing
 * @since 2.5.0
 */
public abstract class JsonFlowParser extends BaseJsonFlowParser {

	/**
	 * 解析一个chain的过程
	 */
	public void parseOneChain(JSONObject chainObject) {
		ParserHelper.parseOneChain(chainObject);
	}

}
