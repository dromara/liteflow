package com.yomahub.liteflow.spi.holder;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.spi.DeclComponentParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 声明式组件解析SPI工厂类
 * @author Bryan.Zhang
 * @since 2.11.4
 */
public class DeclComponentParserHolder {

    private static DeclComponentParser declComponentParser;

    public static DeclComponentParser loadDeclComponentParser(){
        if (ObjectUtil.isNull(declComponentParser)) {
            List<DeclComponentParser> list = new ArrayList<>();
            ServiceLoader.load(DeclComponentParser.class).forEach(list::add);
            list.sort(Comparator.comparingInt(DeclComponentParser::priority));
            declComponentParser = list.get(0);
        }
        return declComponentParser;
    }

    public static void clean(){
        declComponentParser = null;
    }
}
