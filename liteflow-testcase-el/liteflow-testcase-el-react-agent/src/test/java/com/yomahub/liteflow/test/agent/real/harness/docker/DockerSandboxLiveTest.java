package com.yomahub.liteflow.test.agent.real.harness.docker;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.real.RealAgentTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

/**
 * guide §12 DOCKER 文件系统后端的真实模型验证：
 * 真实 Docker 沙箱容器内执行模型驱动的命令。
 *
 * <p>前置条件：本机 Docker daemon 可用且已有 python:3.14-slim 镜像（离线环境无法
 * 拉取 ubuntu:22.04 默认镜像），否则整个类跳过。
 */
@TestPropertySource("classpath:/real/docker-sandbox/application.properties")
@SpringBootTest(classes = DockerSandboxLiveTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.real.harness.docker")
public class DockerSandboxLiveTest extends RealAgentTestBase {

    private static final String IMAGE = "python:3.14-slim";

    @BeforeAll
    static void requireDockerAndImage() throws Exception {
        Process check = new ProcessBuilder("docker", "image", "inspect", IMAGE)
                .redirectErrorStream(true).start();
        boolean ok = check.waitFor(30, TimeUnit.SECONDS) && check.exitValue() == 0;
        Assumptions.assumeTrue(ok, "docker daemon or local image " + IMAGE + " unavailable");
        // Docker 工作区投影前置校验要求 host workspace 目录已存在
        java.nio.file.Files.createDirectories(
                java.nio.file.Path.of("target/wk/real_docker_sandbox"));
    }

    /** §12 DOCKER 后端：模型驱动命令在真实容器内执行并回传输出。 */
    @Test
    public void modelRunsCommandInsideRealDockerSandbox() {
        LiteflowResponse response = flowExecutor.execute2Resp("realDockerSandboxChain",
                "请使用 execute 工具执行命令 echo DOCKER-SANDBOX-OK，"
                        + "然后把命令输出原样告诉我。");

        Assertions.assertTrue(response.isSuccess(), cause(response));
        Object data = response.getSlot().getResponseData();
        Assertions.assertTrue(String.valueOf((Object) data).contains("DOCKER-SANDBOX-OK"),
                "reply must quote sandbox output, got: " + data);
    }

    private static String cause(LiteflowResponse response) {
        if (response.getCause() == null) {
            return "";
        }
        StringBuilder messages = new StringBuilder();
        for (Throwable current = response.getCause();
                current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(" <- ");
        }
        return messages.toString();
    }
}
