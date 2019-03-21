package com.thebeastshop.liteflow.util;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class IOUtil {
    public static String read(String path, String encoding) throws Exception {
        String content = null;

        InputStream inputStream = null;
        try {
            // 从Resource路径获取
            inputStream = IOUtil.class.getClassLoader().getResourceAsStream(path);
            if (inputStream == null) {
                // 从文件路径获取
                inputStream = new FileInputStream(path);
            }
            content = IOUtils.toString(inputStream, encoding);
        } finally {
            if (inputStream != null) {
                IOUtils.closeQuietly(inputStream);
            }
        }

        return content;
    }
}