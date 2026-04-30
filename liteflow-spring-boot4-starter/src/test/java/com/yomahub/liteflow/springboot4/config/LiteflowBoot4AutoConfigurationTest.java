package com.yomahub.liteflow.springboot4.config;

import com.yomahub.liteflow.enums.ParseModeEnum;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.springboot4.LiteflowProperty;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class LiteflowBoot4AutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LiteflowPropertyAutoConfiguration.class));

	@Test
	void bindLiteflowConfigWithSpringBoot4AutoConfiguration() {
		contextRunner
			.withPropertyValues(
				"liteflow.slot-size=16",
				"liteflow.parse-mode=PARSE_ONE_ON_FIRST_EXEC",
				"liteflow.monitor.enable-log=true",
				"liteflow.chain-cache.enabled=true",
				"liteflow.chain-cache.capacity=32")
			.run(context -> {
				assertThat(context).hasSingleBean(LiteflowProperty.class);
				assertThat(context).hasSingleBean(LiteflowConfig.class);

				LiteflowConfig liteflowConfig = context.getBean(LiteflowConfig.class);
				assertThat(liteflowConfig.getSlotSize()).isEqualTo(16);
				assertThat(liteflowConfig.getParseMode()).isEqualTo(ParseModeEnum.PARSE_ONE_ON_FIRST_EXEC);
				assertThat(liteflowConfig.getEnableLog()).isTrue();
				assertThat(liteflowConfig.getChainCacheEnabled()).isTrue();
				assertThat(liteflowConfig.getChainCacheCapacity()).isEqualTo(32);
			});
	}

	@Test
	void publishBoot4AutoConfigurationImportsOnly() throws Exception {
		Path classesPath = classesPath();
		Path importsPath = classesPath.resolve("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");

		assertThat(importsPath).exists();
		assertThat(Files.readAllLines(importsPath)).containsExactly(
			"com.yomahub.liteflow.springboot4.config.LiteflowPropertyAutoConfiguration",
			"com.yomahub.liteflow.springboot4.config.LiteflowMainAutoConfiguration");
		assertThat(classesPath.resolve("META-INF/spring.factories")).doesNotExist();
	}

	private Path classesPath() throws Exception {
		URI uri = LiteflowPropertyAutoConfiguration.class.getProtectionDomain()
			.getCodeSource()
			.getLocation()
			.toURI();
		Path path = Paths.get(uri);
		if (Files.isRegularFile(path)) {
			return path.getParent();
		}
		return path;
	}
}
