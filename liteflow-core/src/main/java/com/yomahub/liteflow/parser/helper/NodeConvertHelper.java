package com.yomahub.liteflow.parser.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;

import java.util.List;

/**
 * 冒号形式节点NodeSimpleVO转换 通用 Helper
 *
 * @author hxinyu
 */
public class NodeConvertHelper {

    public static NodeSimpleVO convert(String scriptKey){
        // 不需要去理解这串正则，就是一个匹配冒号的
        // 一定得是a:b，或是a:b:c...这种完整类型的字符串的
        List<String> matchItemList = ReUtil.findAllGroup0("(?<=[^:]:)[^:]+|[^:]+(?=:[^:])", scriptKey);
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

        if (matchItemList.size() > 4) {
            nodeSimpleVO.setEnable(Boolean.TRUE.toString().equalsIgnoreCase(matchItemList.get(4)));
        }

        return nodeSimpleVO;
    }


    public static class NodeSimpleVO {

        private String nodeId;

        private String type;

        private String name = StrUtil.EMPTY;

        private String language;

        private Boolean enable = Boolean.TRUE;

        private String script;

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

        public Boolean getEnable() {
            return enable;
        }

        public void setEnable(Boolean enable) {
            this.enable = enable;
        }

        public String getScript() {
            return script;
        }

        public void setScript(String script) {
            this.script = script;
        }
    }
}