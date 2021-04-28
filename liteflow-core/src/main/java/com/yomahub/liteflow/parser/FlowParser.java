package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import com.yomahub.liteflow.entity.flow.Condition;
import com.yomahub.liteflow.entity.flow.ThenCondition;
import com.yomahub.liteflow.entity.flow.WhenCondition;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.ConfigErrorException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author guodongqing
 * @since 2.5.0
 */
public abstract class FlowParser {

    public abstract void parseMain(String path) throws Exception;

    public abstract void parse(String content) throws Exception ;

    //条件节点的正则解析
    public static RegexEntity parseNodeStr(String str) {
        List<String> list = new ArrayList<String>();
        Pattern p = Pattern.compile("[^\\)\\(]+");
        Matcher m = p.matcher(str);
        while(m.find()){
            list.add(m.group());
        }
        RegexEntity regexEntity = new RegexEntity();
        regexEntity.setItem(list.get(0).trim());
        if(list.size() > 1){
            String[] realNodeArray = list.get(1).split("\\|");
            for (int i = 0; i < realNodeArray.length; i++) {
                realNodeArray[i] = realNodeArray[i].trim();
            }
            regexEntity.setRealItemArray(realNodeArray);
        }
        return regexEntity;
    }

    protected void buildBaseFlowConditions(List<Condition> conditionList,Condition condition){
        if (condition.getConditionType().equals(ConditionTypeEnum.TYPE_THEN.getType())) {
            if (conditionList.size() > 1 &&
                    CollectionUtil.getLast(conditionList) instanceof ThenCondition) {
                CollectionUtil.getLast(conditionList).getNodeList().addAll(condition.getNodeList());
            } else {
                conditionList.add(new ThenCondition(condition));
            }
        } else if (condition.getConditionType().equals(ConditionTypeEnum.TYPE_WHEN.getType())) {
            if (conditionList.size() > 1 &&
                    CollectionUtil.getLast(conditionList) instanceof WhenCondition &&
                    CollectionUtil.getLast(conditionList).getGroup().equals(condition.getGroup())) {
                CollectionUtil.getLast(conditionList).getNodeList().addAll(condition.getNodeList());
            } else {
                conditionList.add(new WhenCondition(condition));
            }
        }
    }
    
    /**
     *  根据配置的ruleSource查找匹配的资源
     * @param ruleSource 
     * @return
     * @throws IOException
     */
    protected Resource[] matchRuleResources(final String ruleSource)  throws IOException {
        Assert.notNull(ruleSource, "rule source must not be null");
        String locationPattern = ruleSource;
        if (!locationPattern.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
            locationPattern = ResourceUtils.CLASSPATH_URL_PREFIX + locationPattern;
        }
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(locationPattern);
        if(ArrayUtil.isEmpty(resources)) {
            throw new ConfigErrorException("config error,please check rule source property");
        }
        return resources;
    }
}
