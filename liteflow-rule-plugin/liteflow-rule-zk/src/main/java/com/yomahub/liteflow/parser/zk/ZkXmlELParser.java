package com.yomahub.liteflow.parser.zk;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.zk.exception.ZkException;
import com.yomahub.liteflow.parser.zk.util.ZkParserHelper;
import com.yomahub.liteflow.parser.zk.vo.ZkParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Objects;

/**
 * ZK解析器实现，只支持EL形式的XML，不支持其他的形式
 *
 * @author Bryan.Zhang
 * @since 2.8.6
 */
public class ZkXmlELParser extends ClassXmlFlowELParser {

	private final ZkParserHelper zkParserHelper;

	public ZkXmlELParser() {
		LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

		try {
			ZkParserVO zkParserVO = null;
			if (MapUtil.isNotEmpty((liteflowConfig.getRuleSourceExtDataMap()))) {
				zkParserVO = BeanUtil.toBean(liteflowConfig.getRuleSourceExtDataMap(), ZkParserVO.class,
						CopyOptions.create());
			}
			else if (StrUtil.isNotBlank(liteflowConfig.getRuleSourceExtData())) {
				zkParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), ZkParserVO.class);
			}

			if (Objects.isNull(zkParserVO)) {
				throw new ZkException("rule-source-ext-data is empty");
			}

			if (StrUtil.isBlank(zkParserVO.getChainPath())) {
				throw new ZkException("You must configure the chainPath property");
			}
			if (StrUtil.isBlank(zkParserVO.getConnectStr())) {
				throw new ZkException("zk connect string is empty");
			}

			zkParserHelper = new ZkParserHelper(zkParserVO);
		}
		catch (Exception e) {
			throw new ZkException(e);
		}
	}

	@Override
	public String parseCustom() {
		try {
			String content = zkParserHelper.getContent();

			FlowInitHook.addHook(() -> {
				zkParserHelper.listenZkNode();
				return true;
			});

			return content;
		}
		catch (Exception e) {
			throw new ZkException(e);
		}
	}

}
