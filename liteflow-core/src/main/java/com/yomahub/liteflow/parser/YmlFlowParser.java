package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

/**
 * Yml格式解析器，转换为json格式进行解析
 * @author guodongqing
 * @since 2.5.0
 */
public abstract class YmlFlowParser extends JsonFlowParser{

    private final Logger LOG = LoggerFactory.getLogger(YmlFlowParser.class);

    @Override
    public void parse(String content) throws Exception{
        parse(ListUtil.toList(content));
    }

    @Override
    public void parse(List<String> contentList) throws Exception {
        if (CollectionUtil.isEmpty(contentList)) {
            return;
        }

        List<JSONObject> jsonObjectList = ListUtil.toList();
        for (String content : contentList){
            JSONObject ruleObject = convertToJson(content);
            jsonObjectList.add(ruleObject);
        }

        super.parseJsonObject(jsonObjectList);
    }

    protected JSONObject convertToJson(String yamlString) {
        Yaml yaml= new Yaml();
        Map<String, Object> map = yaml.load(yamlString);
        return JSON.parseObject(JSON.toJSONString(map));
    }
}
