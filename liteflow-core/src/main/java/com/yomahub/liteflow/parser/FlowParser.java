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

    //根据配置的ruleSource查找匹配的资源
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
