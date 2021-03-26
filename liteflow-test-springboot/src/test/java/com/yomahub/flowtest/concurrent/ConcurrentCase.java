package com.yomahub.flowtest.concurrent;

import org.junit.Assert;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 流程的顺序执行、并发执行的CASE构造器
 * @author justin.xu
 */
public class ConcurrentCase {
    public static final Map<String, AbstractMap.SimpleEntry<List<Routers>, List<Routers>>> CASES = new ConcurrentHashMap<>();

    /**
     * 初始化一个测试用例的预期
     * @param request
     * @param expected
     */
    public static void caseInit(String request, List<Routers> expected) {
        CASES.put(request, new AbstractMap.SimpleEntry<List<Routers>, List<Routers>>(expected, new CopyOnWriteArrayList<>()));
    }

    /**
     * 添加这个测试用例的实际
     * @param request
     * @param actual
     */
    public static void caseAdd(String request, Routers actual) {
        CASES.computeIfPresent(request, (k, v) -> {
            v.getValue().add(actual);
            return v;
        });
    }

    /**
     * 测试当前的Expected与Actual是否相同
     *
     * @param request
     */
    public static void caseAssert(String request) {
        AbstractMap.SimpleEntry<List<Routers>, List<Routers>> ca = CASES.get(request);
        Assert.assertNotNull(ca);

        Assert.assertEquals(ca.getKey(), ca.getValue());

        if (ca.getValue().size() > 0) {
            Integer expectedIndex = null;
            for (Routers actual : ca.getValue()) {

                if (expectedIndex == null) {
                    expectedIndex = actual.getIndex();
                } else {
                    Assert.assertEquals(expectedIndex.intValue(), actual.getIndex());
                }
            }
        }
    }

    /**
     * 测试当前的Expected与Actual是否相同
     *
     * @param request
     */
    public static void caseAssertRandom(String request) {
        AbstractMap.SimpleEntry<List<Routers>, List<Routers>> ca = CASES.get(request);
        Assert.assertNotNull(ca);

        Assert.assertEquals(ca.getKey().size(), ca.getValue().size());

        if (ca.getValue().size() > 0) {
            Integer expectedIndex = null;
            for (Routers actual : ca.getValue()) {
                boolean find = false;
                for(Routers routers : ca.getKey()) {
                    if (routers.getValue().equals(actual.getValue())) {
                        find = true;
                    }
                }
                Assert.assertTrue(find);

                if (expectedIndex == null) {
                    expectedIndex = actual.getIndex();
                } else {
                    Assert.assertEquals(expectedIndex.intValue(), actual.getIndex());
                }
            }
        }
    }


    public static class Routers {
        int index;
        String value;

        public Routers(String value) {
            this.index = -1;
            this.value = value;
        }
        public Routers(int index, String value) {
            this.index = index;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Routers routers = (Routers) o;
            return value.equals(routers.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}
