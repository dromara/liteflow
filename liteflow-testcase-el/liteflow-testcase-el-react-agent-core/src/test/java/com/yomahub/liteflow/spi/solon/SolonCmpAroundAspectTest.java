package com.yomahub.liteflow.spi.solon;

import com.yomahub.liteflow.spi.CmpAroundAspect;
import org.junit.jupiter.api.Test;
import org.noear.solon.Solon;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolonCmpAroundAspectTest {

    @Test
    void serviceLoaderIsSafeWhenSolonIsOnlyPresentOnAnotherFrameworksClasspath() {
        assertNull(Solon.context());
        List<CmpAroundAspect> providers = new ArrayList<>();

        assertDoesNotThrow(() -> ServiceLoader.load(CmpAroundAspect.class)
                .forEach(providers::add));

        assertTrue(providers.stream().anyMatch(SolonCmpAroundAspect.class::isInstance));
    }
}
