package com.yomahub.liteflow.parser;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

/**
 * Yaml格式转换
 * @Author: guodongqing
 * @Date: 2021/3/26 12:56 下午
 */
public class LocalYmlFlowParser extends JsonFlowParser{

    @Override
    public void parseMain(String rulePath) throws Exception {
        String ruleContent = FileUtil.readUtf8String(rulePath);
        JSONObject ruleObject = convertToJson(ruleContent);
        parse(ruleObject.toJSONString());
    }

    private JSONObject convertToJson(String yamlString) {
        Yaml yaml= new Yaml();
        Map<String, Object> map = yaml.load(yamlString);
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject;
    }
}
