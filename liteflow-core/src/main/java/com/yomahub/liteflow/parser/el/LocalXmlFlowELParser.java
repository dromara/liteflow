package com.yomahub.liteflow.parser.el;

import com.yomahub.liteflow.monitor.MonitorFile;
import com.yomahub.liteflow.spi.holder.PathContentParserHolder;

import java.util.List;

/**
 * 基于本地的xml方式EL表达式解析器
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class LocalXmlFlowELParser extends XmlFlowELParser{
    @Override
    public void parseMain(List<String> pathList) throws Exception {
        List<String> contentList = PathContentParserHolder.loadContextAware().parseContent(pathList);

        // 添加规则文件监听
        List<String> fileAbsolutePath = PathContentParserHolder.loadContextAware().getFileAbsolutePath(pathList);
        MonitorFile.getInstance().addMonitorFilePaths(fileAbsolutePath);

        parse(contentList);
    }
}
