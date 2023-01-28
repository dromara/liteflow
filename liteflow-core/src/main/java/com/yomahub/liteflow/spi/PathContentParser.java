package com.yomahub.liteflow.spi;

import java.util.List;

public interface PathContentParser extends SpiPriority{

    List<String> parseContent(List<String> pathList) throws Exception;

    List<String> getFileAbsolutePath(List<String> pathList) throws Exception;
}
