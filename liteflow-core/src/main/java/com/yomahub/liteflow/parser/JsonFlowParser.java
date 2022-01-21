package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.yomahub.liteflow.builder.LiteFlowChainBuilder;
import com.yomahub.liteflow.builder.LiteFlowConditionBuilder;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.common.LocalDefaultFlowConstant;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.flow.*;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.EmptyConditionValueException;
import com.yomahub.liteflow.exception.ExecutableItemNotFoundException;
import com.yomahub.liteflow.exception.NodeTypeNotSupportException;
import com.yomahub.liteflow.exception.NotSupportConditionException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.spring.ComponentScanner;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

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
        for (Map.Entry<String, NodeComponent> componentEntry : ComponentScanner.nodeComponentMap.entrySet()) {
            if (!FlowBus.containNode(componentEntry.getKey())) {
                FlowBus.addSpringScanNode(componentEntry.getKey(), componentEntry.getValue());
            }
        }

        //先在元数据里放上chain
        //先放有一个好处，可以在parse的时候先映射到FlowBus的chainMap，然后再去解析
        //这样就不用去像之前的版本那样回归调用
        //同时也解决了不能循环依赖的问题
        flowJsonObjectList.forEach(jsonObject -> {
            // 解析chain节点
            JSONArray chainArray = jsonObject.getJSONObject("flow").getJSONArray("chain");

            //先在元数据里放上chain
            chainArray.forEach(o -> {
                JSONObject innerJsonObject = (JSONObject)o;
                FlowBus.addChain(innerJsonObject.getString("name"));
            });
        });

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
                    script = nodeObject.getString("value");
                    file = nodeObject.getString("file");

                    //初始化type
                    if (StrUtil.isBlank(type)){
                        type = NodeTypeEnum.COMMON.getCode();
                    }

                    //检查nodeType是不是规定的类型
                    NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
                    if (ObjectUtil.isNull(nodeTypeEnum)){
                        throw new NodeTypeNotSupportException(StrUtil.format("type [{}] is not support", type));
                    }

                    //进行node的build过程
                    LiteFlowNodeBuilder.createNode().setId(id)
                            .setName(name)
                            .setClazz(clazz)
                            .setType(nodeTypeEnum)
                            .setScript(script)
                            .setFile(file)
                            .build();
                }
            }

            //解析每一个chain
            JSONArray chainArray = flowJsonObject.getJSONObject("flow").getJSONArray("chain");
            chainArray.forEach(o -> {
                JSONObject jsonObject = (JSONObject)o;
                parseOneChain(jsonObject);
            });
        }
    }

    /**
     * 解析一个chain的过程
     */
    private void parseOneChain(JSONObject chainObject){
        String condValueStr;
        ConditionTypeEnum conditionType;
        String group;
        String errorResume;
        String any;

        //构建chainBuilder
        String chainName = chainObject.getString("name");
        LiteFlowChainBuilder chainBuilder = LiteFlowChainBuilder.createChain().setChainName(chainName);

        for (Object o : chainObject.getJSONArray("condition")) {
            JSONObject condObject = (JSONObject) o;
            conditionType = ConditionTypeEnum.getEnumByCode(condObject.getString("type"));
            condValueStr = condObject.getString("value");
            errorResume = condObject.getString("errorResume");
            group = condObject.getString("group");
            any = condObject.getString("any");

            if (ObjectUtil.isNull(conditionType)){
                throw new NotSupportConditionException("ConditionType is not supported");
            }

            if (StrUtil.isBlank(condValueStr)) {
                throw new EmptyConditionValueException("Condition value cannot be empty");
            }


            //如果是when类型的话，有特殊化参数要设置，只针对于when的
            if (conditionType.equals(ConditionTypeEnum.TYPE_WHEN)){
                chainBuilder.setCondition(
                        LiteFlowConditionBuilder.createWhenCondition()
                                .setErrorResume(errorResume)
                                .setGroup(group)
                                .setAny(any)
                                .setValue(condValueStr)
                                .build()
                ).build();
            }else{
                chainBuilder.setCondition(
                        LiteFlowConditionBuilder.createCondition(conditionType)
                                .setValue(condValueStr)
                                .build()
                ).build();
            }
        }
    }
}
