package com.yomahub.liteflow.parser;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.yomahub.liteflow.common.LocalDefaultFlowConstant;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.flow.*;
import com.yomahub.liteflow.exception.ExecutableItemNotFoundException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.spring.ComponentScanner;
import com.yomahub.liteflow.util.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Json格式解析器
 * @author guodongqing
 * @since 2.5.0
 */
public abstract class JsonFlowParser extends FlowParser{

    private final Logger LOG = LoggerFactory.getLogger(JsonFlowParser.class);

    @Override
    public void parse(String content) throws Exception {
        if (StrUtil.isBlank(content)){
            return;
        }

        //把字符串原生转换为json对象，如果不加第二个参数OrderedField，会无序
        JSONObject flowJsonObject = JSONObject.parseObject(content, Feature.OrderedField);
        parse(flowJsonObject);
    }

    //json格式，解析过程
    public void parse(JSONObject flowJsonObject) throws Exception {
        try {
            //判断是以spring方式注册节点，还是以json方式注册
            if(ComponentScanner.nodeComponentMap.isEmpty()){
                JSONArray nodeArrayList = flowJsonObject.getJSONObject("flow").getJSONObject("nodes").getJSONArray("node");
                String id;
                String clazz;
                for(int i = 0; i< nodeArrayList.size(); i++) {
                    JSONObject nodeObject = nodeArrayList.getJSONObject(i);
                    id = nodeObject.getString("id");
                    clazz = nodeObject.getString("class");
                    FlowBus.addNode(id, clazz);
                }
            } else {
                for(Map.Entry<String, NodeComponent> componentEntry : ComponentScanner.nodeComponentMap.entrySet()){
                    if(!FlowBus.containNode(componentEntry.getKey())){
                        FlowBus.addNode(componentEntry.getKey(), new Node(componentEntry.getKey(), componentEntry.getValue().getClass().getName(), componentEntry.getValue()));
                    }
                }
            }

            // 解析chain节点
            JSONArray chainList = flowJsonObject.getJSONObject("flow").getJSONArray("chain");
            Map<String, JSONObject> chainMap = new HashMap<>();
            for(int i=0; i<chainList.size(); i++) {
                JSONObject chainObject = chainList.getJSONObject(i);
                if(chainObject.containsKey("name") && StrUtil.isNotBlank(chainObject.getString("name"))) {
                    chainMap.put(chainObject.getString("name"), chainObject);
                }
            }

            for(Map.Entry<String, JSONObject> chainEntry : chainMap.entrySet()) {
                parseOneChain(chainEntry.getValue(), chainMap);
            }
        } catch (Exception e) {
            LOG.error("JsonFlowParser parser exception", e);
            throw e;
        }
    }

    /**
     * 解析一个chain的过程
     * @param chainObject
     * @param chainMap
     * @throws Exception
     */
    private void parseOneChain(JSONObject chainObject, Map<String, JSONObject> chainMap) throws Exception{
        String condArrayStr;
        String[] condArray;
        List<Executable> chainNodeList;
        List<Condition> conditionList;
        String group;
        String errorResume;
        Condition condition;
        String chainName = chainObject.getString("name");
        JSONArray conditionArray = chainObject.getJSONArray("condition");
        conditionList = new ArrayList<>();
        for(Iterator<Object> iterator = conditionArray.iterator(); iterator.hasNext();) {
            JSONObject condObject = (JSONObject) iterator.next();
            String condType = condObject.getString("type");
            condArrayStr = condObject.getString("value");
            group = condObject.getString("group");
            errorResume = condObject.getString("errorResume");
            if (StrUtil.isBlank(condType) || StrUtil.isBlank(condArrayStr)) {
                continue;
            }
            if (StrUtil.isBlank(group)) {
                group = LocalDefaultFlowConstant.DEFAULT;
            }
            if (StrUtil.isBlank(errorResume)) {
                errorResume = Boolean.TRUE.toString();
            }
            condition = new Condition();
            chainNodeList = new ArrayList<>();
            condArray = condArrayStr.split(",");
            RegexEntity regexEntity;
            String itemExpression;
            String item;
            //这里解析的规则，优先按照node去解析，再按照chain去解析
            for (int i = 0; i < condArray.length; i++) {
                itemExpression = condArray[i].trim();
                regexEntity = parseNodeStr(itemExpression);
                item = regexEntity.getItem();
                if (FlowBus.containNode(item)) {
                    Node node = FlowBus.getNode(item);
                    chainNodeList.add(node);
                    //这里判断是不是条件节点，条件节点会含有realItem，也就是括号里的node
                    if (regexEntity.getRealItemArray() != null) {
                        for (String key : regexEntity.getRealItemArray()) {
                            if (FlowBus.containNode(key)) {
                                Node condNode = FlowBus.getNode(key);
                                node.setCondNode(condNode.getId(), condNode);
                            } else if (hasChain(chainMap, key)) {
                                Chain chain = FlowBus.getChain(key);
                                node.setCondNode(chain.getChainName(), chain);
                            }
                        }
                    }
                } else if(hasChain(chainMap,item)){
                    Chain chain = FlowBus.getChain(item);
                    chainNodeList.add(chain);
                }
                else {
                    String errorMsg = StrUtil.format("executable node[{}] is not found!", regexEntity.getItem());
                    throw new ExecutableItemNotFoundException(errorMsg);
                }
            }
            condition.setErrorResume(errorResume.equals(Boolean.TRUE.toString()));
            condition.setGroup(group);
            condition.setConditionType(condType);
            condition.setNodeList(chainNodeList);
            super.buildBaseFlowConditions(conditionList,condition);
        }
        FlowBus.addChain(chainName, new Chain(chainName,conditionList));
    }

    /**
     * 判断在这个FlowBus元数据里是否含有这个chain
     * 因为chain和node都是可执行器，在一个规则文件上，有可能是node，有可能是chain
     * @param chainMap
     * @param chainName
     * @return
     */
    private boolean hasChain(Map<String, JSONObject> chainMap, String chainName) throws Exception {
        if(chainMap.containsKey(chainName) && !FlowBus.containChain(chainName)) {
            parseOneChain(chainMap.get(chainName), chainMap);
            return true;
        } else if(FlowBus.containChain(chainName)) {
            return true;
        }
        return false;
    }
}
