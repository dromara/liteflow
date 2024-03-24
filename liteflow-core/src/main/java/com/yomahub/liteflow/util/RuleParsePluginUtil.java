package com.yomahub.liteflow.util;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;

/**
 * 插件通用工具类
 *
 * @author gaibu
 */
public class RuleParsePluginUtil {

    private static final String CHAIN_XML_PATTERN = "<chain id=\"{}\" enable=\"{}\">{}</chain>";
    private static final String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" enable=\"{}\"><![CDATA[{}]]></node>";
    private static final String NODE_ITEM_WITH_LANGUAGE_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" language=\"{}\" enable=\"{}\"><![CDATA[{}]]></node>";

    public static ChainDto parseChainKey(String chainKey) {
        String[] chainProp = chainKey.split(StrPool.COLON);
        if (chainProp.length == 2) {
            return new ChainDto(chainProp[0], chainProp[1]);
        }
        return new ChainDto(chainKey);
    }

    public static String toScriptXml(NodeConvertHelper.NodeSimpleVO simpleVO) {
        if (StrUtil.isNotBlank(simpleVO.getLanguage())) {
            return StrUtil.format(NODE_ITEM_WITH_LANGUAGE_XML_PATTERN,
                    simpleVO.getNodeId(),
                    simpleVO.getName(),
                    simpleVO.getType(),
                    simpleVO.getLanguage(),
                    simpleVO.getEnable(),
                    simpleVO.getScript()
            );
        } else {
            return StrUtil.format(NODE_ITEM_XML_PATTERN,
                    simpleVO.getNodeId(),
                    simpleVO.getName(),
                    simpleVO.getType(),
                    simpleVO.getEnable(),
                    simpleVO.getScript()
            );
        }
    }

    public static Pair<Boolean/*启停*/, String/*id*/> parseIdKey(String idKey) {
        String[] idProp = idKey.split(StrPool.COLON);
        if (idProp.length == 2) {
            String id = idProp[0];
            String enableStr = idProp[1];
            return new Pair<>(Boolean.TRUE.toString().equalsIgnoreCase(enableStr), id);
        }
        return new Pair<>(Boolean.TRUE, idKey);
    }

    public static class ChainDto {
        /**
         * chain id
         */
        private String id;

        /**
         * chain 启用状态，默认启用
         */
        private String enable = Boolean.TRUE.toString();

        public boolean isDisable() {
            return !isEnable();
        }

        public boolean isEnable() {
            return Boolean.TRUE.toString().equalsIgnoreCase(enable);
        }

        public ChainDto(String chainId) {
            ChainDto chainDto = new ChainDto(chainId, null);
            this.enable = chainDto.getEnable();
            this.id = chainDto.getId();
        }

        public ChainDto(String chainId, String enable) {
            this.id = chainId;
            if (StrUtil.isBlank(enable)) {
                this.enable = Boolean.TRUE.toString();
                return;
            }
            if (Boolean.TRUE.toString().equalsIgnoreCase(enable)) {
                this.enable = Boolean.TRUE.toString();
                return;
            }
            this.enable = Boolean.FALSE.toString();
        }

        public String getId() {
            return id;
        }

        public String getEnable() {
            return enable;
        }

        public String toElXml(String elContent) {
            return StrUtil.format(CHAIN_XML_PATTERN, id, enable, elContent);
        }
    }
}
