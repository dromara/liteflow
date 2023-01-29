package com.yomahub.liteflow.spi.spring;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.spi.PathContentParser;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ResourceUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpringPathContentParser implements PathContentParser {
    @Override
    public List<String> parseContent(List<String> pathList) throws Exception {
        if(CollectionUtil.isEmpty(pathList)){
            throw new ConfigErrorException("rule source must not be null");
        }

        List<Resource> allResource = new ArrayList<>();
        for (String path : pathList) {
            String locationPattern;

            //如果path是绝对路径且这个文件存在时，我们认为这是一个本地文件路径，而并非classpath路径
            if (FileUtil.isAbsolutePath(path) && FileUtil.isFile(path)){
                locationPattern = ResourceUtils.FILE_URL_PREFIX + path;
            } else {
                if (!path.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX) && !path.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)) {
                    locationPattern = ResourceUtils.CLASSPATH_URL_PREFIX + path;
                }else{
                    locationPattern = path;
                }
            }

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(locationPattern);
            if (ArrayUtil.isNotEmpty(resources)) {
                allResource.addAll(ListUtil.toList(resources));
            }
        }

        //检查资源都是同一个类型，如果出现不同类型的配置，则抛出错误提示
        Set<String> fileTypeSet = new HashSet<>();
        allResource.forEach(resource -> fileTypeSet.add(FileUtil.extName(resource.getFilename())));
        if (fileTypeSet.size() > 1) {
            throw new ConfigErrorException("config error,please use the same type of configuration");
        }

        //转换成内容List
        List<String> contentList = new ArrayList<>();
        for (Resource resource : allResource) {
            String content = IoUtil.read(resource.getInputStream(), CharsetUtil.CHARSET_UTF_8);
            if (StrUtil.isNotBlank(content)){
                contentList.add(content);
            }
        }

        return contentList;
    }

    @Override
    public int priority() {
        return 1;
    }
}
