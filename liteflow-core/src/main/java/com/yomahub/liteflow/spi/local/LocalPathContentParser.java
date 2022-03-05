package com.yomahub.liteflow.spi.local;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.spi.PathContentParser;

import java.util.ArrayList;
import java.util.List;

public class LocalPathContentParser implements PathContentParser {

    private final static String FILE_URL_PREFIX = "file:";

    private final static String CLASSPATH_URL_PREFIX = "classpath:";

    @Override
    public List<String> parseContent(List<String> pathList) throws Exception {
        if(CollectionUtil.isEmpty(pathList)){
            throw new ConfigErrorException("rule source must not be null");
        }

        List<String> contentList = new ArrayList<>();

        for(String path : pathList){
            if (FileUtil.isAbsolutePath(path) && FileUtil.isFile(path)){
                path = FILE_URL_PREFIX + path;
            } else {
                if (!path.startsWith(CLASSPATH_URL_PREFIX)) {
                    path = CLASSPATH_URL_PREFIX + path;
                }
            }
            String content = ResourceUtil.readUtf8Str(path);
            if (StrUtil.isNotBlank(content)){
                contentList.add(ResourceUtil.readUtf8Str(path));
            }
        }

        return contentList;
    }

    @Override
    public int priority() {
        return 2;
    }
}
