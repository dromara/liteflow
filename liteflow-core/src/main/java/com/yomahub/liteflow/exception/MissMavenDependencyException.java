package com.yomahub.liteflow.exception;

import cn.hutool.core.util.StrUtil;

/**
 * 缺少 maven 依赖异常
 *
 * @author tkc
 * @since 2.12.5
 */
public class MissMavenDependencyException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String TEMPLATE = "miss maven dependency " + "\n" +
            "<dependency>\n" +
            "    <groupId>{groupId}</groupId>\n" +
            "    <artifactId>{artifactId}</artifactId>\n" +
            "    <version>${version}</version>\n" +
            "</dependency>";

    /**
     * 异常信息
     */
    private String message;

    public MissMavenDependencyException(String groupId, String artifactId) {
        this.message = StrUtil.format(TEMPLATE, groupId, artifactId);
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
