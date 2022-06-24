package com.yomahub.liteflow.parser.el;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.builder.prop.NodePropBean;
import com.yomahub.liteflow.exception.ChainDuplicateException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.BaseFlowParser;
import com.yomahub.liteflow.spi.holder.ContextCmpInitHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import static com.yomahub.liteflow.common.ChainConstant.*;

/**
 * JSON形式的EL表达式解析抽象引擎
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public abstract class JsonFlowELParser extends BaseFlowParser {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final Set<String> CHAIN_NAME_SET = new CopyOnWriteArraySet<>();

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
        //先在元数据里放上chain
        //先放有一个好处，可以在parse的时候先映射到FlowBus的chainMap，然后再去解析
        //这样就不用去像之前的版本那样回归调用
        //同时也解决了不能循环依赖的问题
        flowJsonObjectList.forEach(jsonObject -> {
            // 解析chain节点
            JSONArray chainArray = jsonObject.getJSONObject(FLOW).getJSONArray(CHAIN);

            //先在元数据里放上chain
            chainArray.forEach(o -> {
                JSONObject innerJsonObject = (JSONObject) o;
                //校验加载的 chainName 是否有重复的
                //TODO 这里是否有个问题，当混合格式加载的时候，2个同名的Chain在不同的文件里，就不行了
                String chainName = innerJsonObject.getString(NAME);
                if (!CHAIN_NAME_SET.add(chainName)) {
                    throw new ChainDuplicateException(String.format("[chain name duplicate] chainName=%s", chainName));
                }

                FlowBus.addChain(innerJsonObject.getString(NAME));
            });
        });
        // 清空
        CHAIN_NAME_SET.clear();

        for (JSONObject flowJsonObject : flowJsonObjectList) {
            // 当存在<nodes>节点定义时，解析node节点
            if (flowJsonObject.getJSONObject(FLOW).containsKey(NODES)) {
                JSONArray nodeArrayList = flowJsonObject.getJSONObject(FLOW).getJSONObject(NODES).getJSONArray(NODE);
                String id, name, clazz, script, type, file;
                for (int i = 0; i < nodeArrayList.size(); i++) {
                    JSONObject nodeObject = nodeArrayList.getJSONObject(i);
                    id = nodeObject.getString(ID);
                    name = nodeObject.getString(NAME);
                    clazz = nodeObject.getString(_CLASS);
                    type = nodeObject.getString(TYPE);
                    script = nodeObject.getString(VALUE);
                    file = nodeObject.getString(FILE);

                    // 构建 node
                    NodePropBean nodePropBean = new NodePropBean()
                            .setId(id)
                            .setName(name)
                            .setClazz(clazz)
                            .setScript(script)
                            .setType(type)
                            .setFile(file);

                    buildNode(nodePropBean);
                }
            }

            //解析每一个chain
            JSONArray chainArray = flowJsonObject.getJSONObject(FLOW).getJSONArray(CHAIN);
            chainArray.forEach(o -> {
                JSONObject jsonObject = (JSONObject) o;
                parseOneChain(jsonObject);
            });
        }
    }


    /**
     * 解析一个chain的过程
     */
    private void parseOneChain(JSONObject chainObject) {
        //构建chainBuilder
        String chainName = chainObject.getString(NAME);
        String el = chainObject.getString(VALUE);
        LiteFlowChainELBuilder chainELBuilder = LiteFlowChainELBuilder.createChain().setChainName(chainName);
        chainELBuilder.setEL(el).build();
    }
}
