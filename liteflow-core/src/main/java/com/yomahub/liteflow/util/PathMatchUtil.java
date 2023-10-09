package com.yomahub.liteflow.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.AntPathMatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用于获取模糊匹配的路径
 *
 * @author Rain
 * @since 2.11.1
 */
public class PathMatchUtil {

    public static List<String> searchAbsolutePath(List<String> pathList) {

        List<String> absolutePathList = new ArrayList<>();

        for (String path : pathList) {
            // 只对绝对路径进行处理
            if(FileUtil.isAbsolutePath(path)) {
                if(!path.contains("*")) {
                    absolutePathList.add(path);
                }
                else {
                    String[] pathSegments = path.split("/");
                    StringBuilder baseDir = new StringBuilder();

                    // 找到最大基础路径
                    for(int i = 0; i < pathSegments.length; i ++) {
                        if(!pathSegments[i].contains("*")) {
                            baseDir.append(pathSegments[i]).append(File.separator);
                        } else {
                            baseDir.deleteCharAt(baseDir.length() - 1);
                            searchAbsolutePath(baseDir.toString(), path, absolutePathList);
                            break;
                        }
                    }
                }
            } else {
                absolutePathList.add(path);
            }
        }
        // 路径去重
        List<String> newAbsolutePathList = absolutePathList.stream()
                .distinct()
                .collect(Collectors.toList());
        return newAbsolutePathList;
    }

    private static void searchAbsolutePath(String baseDir, String path, List<String> absolutePathList) {
        AntPathMatcher pathMatcher = new AntPathMatcher();
        File dir = new File(baseDir);
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchAbsolutePath(file.getAbsolutePath(), path, absolutePathList);
                } else {
                    String absolutePath = file.getAbsolutePath().replace("\\", "/");
                    if (pathMatcher.match(path, absolutePath)) {
                        absolutePathList.add(absolutePath);
                    }
                }
            }
        }
    }
}
