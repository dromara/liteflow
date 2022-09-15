package com.yomahub.liteflow.parser.nacos;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.nacos.exception.NacosException;
import com.yomahub.liteflow.parser.nacos.util.NacosParserHelper;
import com.yomahub.liteflow.parser.nacos.vo.NacosParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.function.Consumer;

/**
 * Nacos 解析器实现，只支持EL形式的XML，不支持其他的形式
 * @author mll
 * @since 2.9.0
 */
public class NacosXmlELParser extends ClassXmlFlowELParser {

    private final NacosParserHelper helper;

    public NacosXmlELParser() {
        Consumer<String> parseConsumer = t -> {
            try {
                parse(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

        if (StrUtil.isBlank(liteflowConfig.getRuleSourceExtData())){
            throw new NacosException("rule-source-ext-data for nacos is empty");
        }

        try{
            NacosParserVO nacosParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), NacosParserVO.class);
            assert nacosParserVO != null;
            if (StrUtil.isBlank(nacosParserVO.getServerAddr())){
                nacosParserVO.setServerAddr("127.0.0.1:8848");
            }
            if (StrUtil.isBlank(nacosParserVO.getNamespace())){
                nacosParserVO.setNamespace("");
            }
            if (StrUtil.isBlank(nacosParserVO.getDataId())){
                nacosParserVO.setDataId("LiteFLow");
            }
            if (StrUtil.isBlank(nacosParserVO.getGroup())){
                nacosParserVO.setGroup("LITE_FLOW_GROUP");
            }
            if (StrUtil.isBlank(nacosParserVO.getUsername())){
                nacosParserVO.setUsername("");
            }
            if (StrUtil.isBlank(nacosParserVO.getPassword())){
                nacosParserVO.setPassword("");
            }
            helper = new NacosParserHelper(nacosParserVO, parseConsumer);
        }catch (Exception e){
            throw new NacosException(e.getMessage());
        }
    }

    @Override
    public String parseCustom() {
        try{
            String content = helper.getContent();
            helper.checkContent(content);
            helper.listener();
            return content;
        }catch (Exception e){
            throw new NacosException(e.getMessage());
        }
    }
}
