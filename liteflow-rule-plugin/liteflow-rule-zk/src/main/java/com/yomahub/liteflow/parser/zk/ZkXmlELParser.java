package com.yomahub.liteflow.parser.zk;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.zk.exception.ZkException;
import com.yomahub.liteflow.parser.zk.util.ZkParserHelper;
import com.yomahub.liteflow.parser.zk.vo.ZkParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;
import org.apache.curator.framework.CuratorFramework;

import java.util.function.Consumer;

/**
 * ZK解析器实现，只支持EL形式的XML，不支持其他的形式
 * @author Bryan.Zhang
 * @since 2.8.6
 */
public class ZkXmlELParser extends ClassXmlFlowELParser {

    private final ZkParserHelper zkParserHelper;

    public ZkXmlELParser() {
        Consumer<String> parseConsumer = t -> {
            try {
                parse(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

        if (StrUtil.isBlank(liteflowConfig.getRuleSourceExtData())){
            throw new ZkException("rule-source-ext-data is empty");
        }

        try{
            ZkParserVO zkParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), ZkParserVO.class);
            assert zkParserVO != null;

            if (StrUtil.isBlank(zkParserVO.getNodePath())){
                zkParserVO.setNodePath("/lite-flow/flow");
            }
            if (StrUtil.isBlank(zkParserVO.getConnectStr())){
                throw new ZkException("zk connect string is empty");
            }

            zkParserHelper = new ZkParserHelper(zkParserVO, parseConsumer);
        }catch (Exception e){
            throw new ZkException(e.getMessage());
        }
    }

    @Override
    public String parseCustom() {
        try{
            String content = zkParserHelper.getContent();

            zkParserHelper.checkContent(content);

            zkParserHelper.listenZkNode();

            return content;
        }catch (Exception e){
            throw new ZkException(e.getMessage());
        }
    }
}
