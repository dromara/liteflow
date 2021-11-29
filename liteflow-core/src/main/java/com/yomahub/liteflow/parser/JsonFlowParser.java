package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.yomahub.liteflow.common.LocalDefaultFlowConstant;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.flow.*;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ExecutableItemNotFoundException;
import com.yomahub.liteflow.exception.NodeTypeNotSupportException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.spring.ComponentScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Json格式解析器
 *
 * @author guodongqing
 * @since 2.5.0
 */
public abstract class JsonFlowParser extends FlowParser {

    private final Logger LOG = LoggerFactory.getLogger(JsonFlowParser.class);

    public void parse(String content) throws Exception {
        parse(ListUtil.toList(content));
    }

    @Override
    public void parse(List<String> contentList) throws Exception {
        if (CollectionUtil.isEmpty(contentList)) {
            return;
        }

        List<JSONObject> jsonObjectList = ListUtil.toList();
        for (String content : contentList) {
            //把字符串原生转换为json对象，如果不加第二个参数OrderedField，会无序
            JSONObject flowJsonObject = JSONObject.parseObject(content, Feature.OrderedField);
            jsonObjectList.add(flowJsonObject);
        }

        parseJsonObject(jsonObjectList);
    }

    //json格式，解析过程
    public void parseJsonObject(List<JSONObject> flowJsonObjectList) throws Exception {
        try {
            for (Map.Entry<String, NodeComponent> componentEntry : ComponentScanner.nodeComponentMap.entrySet()) {
                if (!FlowBus.containNode(componentEntry.getKey())) {
                    FlowBus.addSpringScanNode(componentEntry.getKey(), componentEntry.getValue());
                }
            }

            for (JSONObject flowJsonObject : flowJsonObjectList) {
                // 当存在<nodes>节点定义时，解析node节点
                if (flowJsonObject.getJSONObject("flow").containsKey("nodes")){
                    JSONArray nodeArrayList = flowJsonObject.getJSONObject("flow").getJSONObject("nodes").getJSONArray("node");
                    String id, name, clazz, script, type, file;
                    for (int i = 0; i < nodeArrayList.size(); i++) {
                        JSONObject nodeObject = nodeArrayList.getJSONObject(i);
                        id = nodeObject.getString("id");
                        name = nodeObject.getString("name");
                        clazz = nodeObject.getString("class");
                        type = nodeObject.getString("type");
                        file = nodeObject.getString("file");

                        //初始化type
                        if (StrUtil.isBlank(type)){
                            type = NodeTypeEnum.COMMON.getCode();
                        }
                        NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
                        if (ObjectUtil.isNull(nodeTypeEnum)){
                            throw new NodeTypeNotSupportException(StrUtil.format("type [{}] is not support", type));
                        }

                        //这里区分是普通java节点还是脚本节点
                        //如果是脚本节点，又区分是普通脚本节点，还是条件脚本节点
                        if (nodeTypeEnum.equals(NodeTypeEnum.COMMON) && StrUtil.isNotBlank(clazz)){
                            FlowBus.addCommonNode(id, name, clazz);
                        }else if(nodeTypeEnum.equals(NodeTypeEnum.SCRIPT) || nodeTypeEnum.equals(NodeTypeEnum.COND_SCRIPT)){
                            //如果file字段不为空，则优先从file里面读取脚本文本
                            if (StrUtil.isNotBlank(file)){
                                script = ResourceUtil.readUtf8Str(StrUtil.format("classpath: {}", file));
                            }else{
                                script = nodeObject.getString("value");
                            }

                            //根据节点类型把脚本添加到元数据里
                            if (nodeTypeEnum.equals(NodeTypeEnum.SCRIPT)){
                                FlowBus.addCommonScriptNode(id, name, script);
                            }else {
                                FlowBus.addCondScriptNode(id, name, script);
                            }
                        }
                    }
                }

                // 解析chain节点
                JSONArray chainArray = flowJsonObject.getJSONObject("flow").getJSONArray("chain");
                for (int i = 0; i < chainArray.size(); i++) {
                    JSONObject jsonObject = chainArray.getJSONObject(i);
                    parseOneChain(jsonObject, flowJsonObjectList);
                }
            }
        } catch (Exception e) {
            LOG.error("JsonFlowParser parser exception", e);
            throw e;
        }
    }

    /**
     * 解析一个chain的过程
     */
    private void parseOneChain(JSONObject chainObject, List<JSONObject> flowJsonObjectList) throws Exception {
        String condArrayStr;
        String[] condArray;
        List<Executable> chainNodeList;
        List<Condition> conditionList = new ArrayList<>();
        String group;
        String errorResume;
        Condition condition;
        String any;
        String chainName = chainObject.getString("name");
        JSONArray conditionArray = chainObject.getJSONArray("condition");
        for (Object o : conditionArray) {
            JSONObject condObject = (JSONObject) o;
            String condType = condObject.getString("type");
            condArrayStr = condObject.getString("value");
            group = condObject.getString("group");
            errorResume = condObject.getString("errorResume");
            any = condObject.getString("any");
            if (StrUtil.isBlank(condType) || StrUtil.isBlank(condArrayStr)) {
                continue;
            }
            if (StrUtil.isBlank(group)) {
                group = LocalDefaultFlowConstant.DEFAULT;
            }
            if (StrUtil.isBlank(errorResume)) {
                errorResume = Boolean.TRUE.toString();
            }
            if (StrUtil.isBlank(any)){
                any = Boolean.FALSE.toString();
            }
            condition = new Condition();
            chainNodeList = new ArrayList<>();
            condArray = condArrayStr.split(",");
            RegexEntity regexEntity;
            String itemExpression;
            RegexNodeEntity item;
            //这里解析的规则，优先按照node去解析，再按照chain去解析
            for (int i = 0; i < condArray.length; i++) {
                itemExpression = condArray[i].trim();
                regexEntity = RegexEntity.parse(itemExpression);
                item = regexEntity.getItem();
                if (FlowBus.containNode(item.getId())) {
                    Node node = FlowBus.copyNode(item.getId());
                    node.setTag(regexEntity.getItem().getTag());
                    chainNodeList.add(node);
                    //这里判断是不是条件节点，条件节点会含有realItem，也就是括号里的node
                    if (regexEntity.getRealItemArray() != null) {
                        for (RegexNodeEntity realItem : regexEntity.getRealItemArray()) {
                            if (FlowBus.containNode(realItem.getId())) {
                                Node condNode = FlowBus.copyNode(realItem.getId());
                                node.setTag(realItem.getTag());
                                node.setCondNode(condNode.getId(), condNode);
                            } else if (hasChain(flowJsonObjectList, realItem.getId())) {
                                Chain chain = FlowBus.getChain(realItem.getId());
                                node.setCondNode(chain.getChainName(), chain);
                            }
                        }
                    }
                } else if (hasChain(flowJsonObjectList, item.getId())) {
                    Chain chain = FlowBus.getChain(item.getId());
                    chainNodeList.add(chain);
                } else {
                    String errorMsg = StrUtil.format("executable node[{}] is not found!", regexEntity.getItem().getId());
                    throw new ExecutableItemNotFoundException(errorMsg);
                }
            }
            condition.setErrorResume(errorResume.equals(Boolean.TRUE.toString()));
            condition.setGroup(group);
            condition.setAny(any.equals(Boolean.TRUE.toString()));
            condition.setConditionType(condType);
            condition.setNodeList(chainNodeList);
            super.buildConditions(conditionList, condition);
        }
        FlowBus.addChain(chainName, new Chain(chainName, conditionList));
    }

    /**
     * 判断在这个FlowBus元数据里是否含有这个chain
     * 因为chain和node都是可执行器，在一个规则文件上，有可能是node，有可能是chain
     */
    private boolean hasChain(List<JSONObject> flowJsonObjectList, String chainName) throws Exception {
        for (JSONObject jsonObject : flowJsonObjectList) {
            JSONArray chainArray = jsonObject.getJSONObject("flow").getJSONArray("chain");
            for (int i = 0; i < chainArray.size(); i++) {
                JSONObject chainObject = chainArray.getJSONObject(i);
                if (chainObject.getString("name").equals(chainName) && !FlowBus.containChain(chainName)) {
                    parseOneChain(chainObject, flowJsonObjectList);
                    return true;
                } else if (FlowBus.containChain(chainName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
