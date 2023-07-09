package com.yomahub.liteflow.parser.redis.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Redis 解析器通用接口
 *
 * @author hxinyu
 * @since  2.10.6
 */

public interface RedisParserHelper {

   LFLog LOG = LFLoggerManager.getLogger(FlowExecutor.class);

    String REDIS_URL_PATTERN = "redis://{}:{}";

    String CHAIN_XML_PATTERN = "<chain name=\"{}\">{}</chain>";

    String NODE_XML_PATTERN = "<nodes>{}</nodes>";

    String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\"><![CDATA[{}]]></node>";

    String NODE_ITEM_WITH_LANGUAGE_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" language=\"{}\"><![CDATA[{}]]></node>";

    String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

    String getContent();

    void listenRedis();


    /**
     * script节点的修改/添加
     * @param scriptFieldValue 新的script名
     * @param newValue 新的script值
     */
    default void changeScriptNode(String scriptFieldValue, String newValue) {
        NodeSimpleVO nodeSimpleVO = convert(scriptFieldValue);
        // 有语言类型
        if (StrUtil.isNotBlank(nodeSimpleVO.getLanguage())) {
            LiteFlowNodeBuilder.createScriptNode()
                    .setId(nodeSimpleVO.getNodeId())
                    .setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
                    .setName(nodeSimpleVO.getName())
                    .setScript(newValue)
                    .setLanguage(nodeSimpleVO.getLanguage())
                    .build();
        }
        // 没有语言类型
        else {
            LiteFlowNodeBuilder.createScriptNode()
                    .setId(nodeSimpleVO.getNodeId())
                    .setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
                    .setName(nodeSimpleVO.getName())
                    .setScript(newValue)
                    .build();
        }
    }

    default NodeSimpleVO convert(String str) {
        // 不需要去理解这串正则，就是一个匹配冒号的
        // 一定得是a:b，或是a:b:c...这种完整类型的字符串的
        List<String> matchItemList = ReUtil.findAllGroup0("(?<=[^:]:)[^:]+|[^:]+(?=:[^:])", str);
        if (CollUtil.isEmpty(matchItemList)) {
            return null;
        }

        NodeSimpleVO nodeSimpleVO = new NodeSimpleVO();
        if (matchItemList.size() > 1) {
            nodeSimpleVO.setNodeId(matchItemList.get(0));
            nodeSimpleVO.setType(matchItemList.get(1));
        }

        if (matchItemList.size() > 2) {
            nodeSimpleVO.setName(matchItemList.get(2));
        }

        if (matchItemList.size() > 3) {
            nodeSimpleVO.setLanguage(matchItemList.get(3));
        }

        return nodeSimpleVO;
    }

    class NodeSimpleVO {

        private String nodeId;

        private String type;

        private String name = StrUtil.EMPTY;

        private String language;

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

    }
}
