package com.yomahub.liteflow.test.agent.unit;

import com.yomahub.liteflow.util.ConversationIdGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 覆盖 guide §5.2 中默认 cid 生成格式：{@code YYYYMMDD_<12 位 NanoId>}。
 */
public class ConversationIdGeneratorTest {

    private static final Pattern FORMAT = Pattern.compile("\\d{8}_[0-9A-Z]{12}");

    @Test
    public void testGenerateMatchesDocumentedFormat() {
        String cid = ConversationIdGenerator.generate();
        Assertions.assertNotNull(cid);
        Assertions.assertTrue(FORMAT.matcher(cid).matches(),
                "cid 应符合 YYYYMMDD_<12 位 NanoId> 格式，实际=" + cid);
    }

    @Test
    public void testGenerateProducesDistinctValues() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 32; i++) {
            generated.add(ConversationIdGenerator.generate());
        }
        Assertions.assertEquals(32, generated.size(),
                "32 次连续生成应该都不同（碰撞概率极低）");
    }
}
