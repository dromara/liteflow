package com.yomahub.liteflow.test.utils;

import cn.hutool.core.lang.Assert;
import com.yomahub.liteflow.util.QlExpressUtils;
import org.junit.jupiter.api.Test;

/**
 * EL 工具类测试
 *
 * @author tangkc
 * @since 2.13.2
 */
public class QlExpressUtilsTest {

    @Test
    public void checkVariableNameTest(){
        // 错误的
        Assert.isFalse(QlExpressUtils.checkVariableName(""));
        Assert.isFalse(QlExpressUtils.checkVariableName("11a"));
        Assert.isFalse(QlExpressUtils.checkVariableName("a-a"));
        // 正确的
        Assert.isTrue(QlExpressUtils.checkVariableName("aa"));
        Assert.isTrue(QlExpressUtils.checkVariableName("_aa"));
        Assert.isTrue(QlExpressUtils.checkVariableName("$aa"));
        Assert.isTrue(QlExpressUtils.checkVariableName("$a_a"));
    }
}
