package com.yomahub.liteflow.test.agent.platform.minimax;

import com.yomahub.liteflow.agent.anthropic.AnthropicCompatible;
import com.yomahub.liteflow.agent.openai.Minimax;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

public class MinimaxEndpointTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    @Test
    public void testOpenAICompatibleRequestPath() throws Exception {
        try (RequestCapture capture = new RequestCapture()) {
            AgentConfig config = new AgentConfig();
            config.getOpenaiCompatible()
                    .put("minimax", credential(capture.baseUrl("/v1")));

            Model model = Minimax.of("MiniMax-M3").stream(false).resolve(config);

            invoke(model);
            Assertions.assertEquals("/v1/chat/completions", capture.awaitPath());
        }
    }

    @Test
    public void testAnthropicCompatibleRequestPath() throws Exception {
        try (RequestCapture capture = new RequestCapture()) {
            AgentConfig config = new AgentConfig();
            config.getAnthropicCompatible()
                    .put("minimax", credential(capture.baseUrl("/anthropic")));

            Model model = AnthropicCompatible.custom("minimax", "MiniMax-M2.7")
                    .stream(false)
                    .resolve(config);

            invoke(model);
            Assertions.assertEquals("/anthropic/v1/messages", capture.awaitPath());
        }
    }

    private static PlatformCredential credential(String baseUrl) {
        PlatformCredential credential = new PlatformCredential();
        credential.setApiKey("test-key");
        credential.setBaseUrl(baseUrl);
        return credential;
    }

    private static void invoke(Model model) {
        Msg message = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .textContent("ping")
                .build();
        GenerateOptions options = GenerateOptions.builder()
                .executionConfig(ExecutionConfig.builder()
                        .timeout(REQUEST_TIMEOUT)
                        .maxAttempts(1)
                        .build())
                .build();

        model.stream(List.of(message), List.of(), options)
                .onErrorResume(error -> Flux.empty())
                .blockLast(REQUEST_TIMEOUT);
    }

    private static final class RequestCapture implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final CompletableFuture<String> requestPath;

        private RequestCapture() throws IOException {
            serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            requestPath = CompletableFuture.supplyAsync(this::captureRequest);
        }

        private String baseUrl(String path) {
            return "http://127.0.0.1:" + serverSocket.getLocalPort() + path;
        }

        private String awaitPath() throws Exception {
            return requestPath.get(REQUEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        }

        private String captureRequest() {
            try (Socket socket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            socket.getInputStream(), StandardCharsets.UTF_8));
                    OutputStream output = socket.getOutputStream()) {
                String requestLine = reader.readLine();
                if (requestLine == null) {
                    throw new IOException("Missing HTTP request line");
                }

                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    // Consume request headers before returning the test response.
                }

                byte[] response = ("HTTP/1.1 400 Bad Request\r\n"
                                + "Content-Type: application/json\r\n"
                                + "Content-Length: 2\r\n"
                                + "Connection: close\r\n"
                                + "\r\n{}")
                        .getBytes(StandardCharsets.UTF_8);
                output.write(response);
                output.flush();

                String[] parts = requestLine.split(" ", 3);
                if (parts.length < 2) {
                    throw new IOException("Invalid HTTP request line: " + requestLine);
                }
                return parts[1];
            } catch (IOException error) {
                throw new IllegalStateException("Failed to capture HTTP request", error);
            }
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }
    }
}
