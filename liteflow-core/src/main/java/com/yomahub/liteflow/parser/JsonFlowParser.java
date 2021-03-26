package com.yomahub.liteflow.parser;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.flow.*;
import com.yomahub.liteflow.exception.ExecutableItemNotFoundException;
import com.yomahub.liteflow.exception.ParseException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.spring.ComponentScaner;
import com.yomahub.liteflow.util.SpringAware;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Json格式解析器
 * @Author: guodongqing
 * @Date: 2021-03-25 16:40:00
 */
public abstract class JsonFlowParser extends FlowParser{

    private final Logger LOG = LoggerFactory.getLogger(JsonFlowParser.class);

    @Override
    public void parse(String content) throws Exception {
        //把字符串原生转换为json对象，如果不加第二个参数OrderedField，会无序
        JSONObject flowJsonObject = JSONObject.parseObject(content, Feature.OrderedField);
        parse(flowJsonObject);
    }

    /**
     * json格式，解析过程
     * @param flowJsonObject
     * @throws Exception
     */
    public void parse(JSONObject flowJsonObject) throws Exception {
        try {
            //判断是以spring方式注册节点，还是以json方式注册
            if(ComponentScaner.nodeComponentMap.isEmpty()){
                JSONArray nodeArrayList = flowJsonObject.getJSONObject("nodes").getJSONArray("node");
                String id;
                String clazz;
                Node node;
                NodeComponent component;
                Class<NodeComponent> nodeComponentClass;
                for(int i = 0; i< nodeArrayList.size(); i++) {
                    JSONObject nodeObject = nodeArrayList.getJSONObject(i);
                    node = new Node();
                    id = nodeObject.getString("id");
                    clazz = nodeObject.getString("class");
                    node.setId(id);
                    node.setClazz(clazz);
                    nodeComponentClass = (Class<NodeComponent>)Class.forName(clazz);
                    component = SpringAware.registerOrGet(nodeComponentClass);
                    if (component == null) {
                        LOG.error("couldn't find component class [{}] ", clazz);
                        throw new ParseException("cannot parse flow json");
                    }
                    component.setNodeId(id);
                    node.setInstance(component);
                    FlowBus.addNode(id, node);
                }
            } else {
                for(Map.Entry<String, NodeComponent> componentEntry : ComponentScaner.nodeComponentMap.entrySet()){
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
                if(chainObject.containsKey("name") && StringUtils.isNotBlank(chainObject.getString("name"))) {
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
        String chainName = chainObject.getString("name");
        JSONArray chainTopoArray = chainObject.getJSONArray("topo");
        conditionList = new ArrayList<>();
        for(Iterator<Object> iterator = chainTopoArray.iterator(); iterator.hasNext();) {
            JSONObject condObject = (JSONObject) iterator.next();
            String condType = condObject.getString("type");
            condArrayStr = condObject.getString("value");
            if (StringUtils.isBlank(condType) || StringUtils.isBlank(condArrayStr)) {
                continue;
            }
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
            if (condType.equals("then")) {
                conditionList.add(new ThenCondition(chainNodeList));
            } else if (condType.equals("when")) {
                conditionList.add(new WhenCondition(chainNodeList));
            }
            FlowBus.addChain(chainName, new Chain(chainName,conditionList));
        }

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
        }
        return false;
    }
}
