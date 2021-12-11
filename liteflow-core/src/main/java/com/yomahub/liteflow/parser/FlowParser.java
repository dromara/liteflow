package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import com.google.common.collect.Lists;
import com.yomahub.liteflow.entity.flow.*;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.ConfigErrorException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.util.*;

/**
 * 虽则Parser的抽象类，所有的parser需要继承这个抽象类
 * @author guodongqing
 * @since 2.5.0
 */
public abstract class FlowParser {

    public abstract void parseMain(List<String> pathList) throws Exception;

    public abstract void parse(List<String> contentList) throws Exception;

    protected void buildConditions(List<Condition> conditionList, Condition condition) {
        //这里进行合并逻辑
        //对于then来说，相邻的2个then会合并成一个condition
        //对于when来说，相同组的when会合并成一个condition，不同组的when还是会拆开
        if (condition.getConditionType().equals(ConditionTypeEnum.TYPE_PRE.getType())) {
            conditionList.add(new PreCondition(condition));
        } else if (condition.getConditionType().equals(ConditionTypeEnum.TYPE_THEN.getType())) {
            if (conditionList.size() >= 1 &&
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
        } else if (condition.getConditionType().equals(ConditionTypeEnum.TYPE_FINALLY.getType())) {
            conditionList.add(new FinallyCondition(condition));
        }

        //每一次build之后，对conditionList进行排序，pre最前面，finally最后
        //这里为什么要排序，因为在声明的时候，哪怕有人不把pre放最前，finally放最后，但最终也要确保是正确的顺序
        CollectionUtil.sort(conditionList, (o1, o2) -> {
            if (o1.getConditionType().equals(ConditionTypeEnum.TYPE_PRE.getType()) || o2.getConditionType().equals(ConditionTypeEnum.TYPE_FINALLY.getType())){
                return -1;
            } else if (o2.getConditionType().equals(ConditionTypeEnum.TYPE_PRE.getType()) || o1.getConditionType().equals(ConditionTypeEnum.TYPE_FINALLY.getType())){
                return 1;
            }
            return 0;
        });
    }

    /**
     * 根据配置的ruleSource查找匹配的资源
     */
    protected Resource[] matchRuleResources(final List<String> pathList) throws IOException {
        Assert.notEmpty(pathList, "rule source must not be null");

        List<Resource> allResource = new ArrayList<>();
        for (String path : pathList) {
            String locationPattern;

            //如果path是绝对路径且这个文件存在时，我们认为这是一个本地文件路径，而并非classpath路径
            if (FileUtil.isAbsolutePath(path) && FileUtil.isFile(path)){
                locationPattern = ResourceUtils.FILE_URL_PREFIX + path;
            } else {
                if (!path.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
                    locationPattern = ResourceUtils.CLASSPATH_URL_PREFIX + path;
                }else{
                    locationPattern = path;
                }
            }

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(locationPattern);
            if (ArrayUtil.isEmpty(resources)) {
                throw new ConfigErrorException("config error,please check rule source property");
            }
            allResource.addAll(ListUtil.toList(resources));
        }

        //如果有多个资源，检查资源都是同一个类型，如果出现不同类型的配置，则抛出错误提示
        Set<String> fileTypeSet = new HashSet<>();
        allResource.forEach(resource -> fileTypeSet.add(FileUtil.extName(resource.getFilename())));
        if (fileTypeSet.size() != 1) {
            throw new ConfigErrorException("config error,please use the same type of configuration");
        }

        return allResource.toArray(new Resource[]{});
    }
}
