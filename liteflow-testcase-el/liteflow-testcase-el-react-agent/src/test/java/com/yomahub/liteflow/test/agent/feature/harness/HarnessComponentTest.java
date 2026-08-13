package com.yomahub.liteflow.test.agent.feature.harness;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import com.yomahub.liteflow.agent.harness.sandbox.SandboxSnapshotProvider;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.state.State;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.sandbox.ExecResult;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.SandboxState;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxState;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshot;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource("classpath:/feature/harness/application.properties")
@SpringBootTest(classes = HarnessComponentTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.harness")
public class HarnessComponentTest {

    @Resource
    private FlowExecutor flowExecutor;

    @BeforeEach
    void resetOfflineFakes() {
        OfflineHarnessFixtures.resetObservations();
    }

    @Test
    void thenRestoresOneConversationWorkspaceAcrossHarnessNodes() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "harnessThenWorkspace",
                "offline request",
                ExecuteOption.of().conversationId("conversation-harness-then"));

        assertTrue(response.isSuccess(), () -> "chain failed: " + causeMessages(response.getCause()));
        assertEquals("conversation-harness-then", response.getConversationId());
        assertEquals("written", response.getSlot().getOutput("harnessWriter"));
        assertEquals("shared-through-snapshot", response.getSlot().getOutput("harnessReaderA"));

        assertEquals(1, OfflineHarnessFixtures.SANDBOX_CLIENT.createCount());
        assertEquals(1, OfflineHarnessFixtures.SANDBOX_CLIENT.resumeCount());
        assertEquals(1, OfflineHarnessFixtures.SNAPSHOTS.restoreCount());
        assertEquals(1, OfflineHarnessFixtures.STATE_STORE.sessionsFor("_sandbox_state").size(),
                "THEN nodes in one conversation must share one physical sandbox-state route");
    }

    @Test
    void whenRoutesSharedConversationSandboxAndSeparateAgentState() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "harnessThenWhen",
                "offline request",
                ExecuteOption.of().conversationId("conversation-harness-when"));

        assertTrue(response.isSuccess(), () -> "chain failed: " + causeMessages(response.getCause()));
        assertEquals("conversation-harness-when", response.getConversationId());
        assertEquals("written", response.getSlot().getOutput("harnessWriter"));
        assertEquals("shared-through-snapshot", response.getSlot().getOutput("harnessReaderA"));
        assertEquals("shared-through-snapshot", response.getSlot().getOutput("harnessReaderB"));

        assertEquals(1, OfflineHarnessFixtures.SANDBOX_CLIENT.createCount());
        assertEquals(2, OfflineHarnessFixtures.SANDBOX_CLIENT.resumeCount());
        assertEquals(2, OfflineHarnessFixtures.SNAPSHOTS.restoreCount());

        Set<String> sandboxSessions = OfflineHarnessFixtures.STATE_STORE.sessionsFor("_sandbox_state");
        Set<String> agentSessions = OfflineHarnessFixtures.STATE_STORE.sessionsFor("agent_state");
        assertEquals(1, sandboxSessions.size(),
                "one conversation must use one physical sandbox-state route across agents");
        assertEquals(3, agentSessions.size(),
                "THEN/WHEN Harness nodes must keep physical AgentState sessions separate");
        assertFalse(agentSessions.containsAll(sandboxSessions),
                "agent state and conversation sandbox state must use different namespaces");
    }

    @Test
    void harnessModelFailureIsReportedByLiteflowResponse() {
        LiteflowResponse response = flowExecutor.execute2Resp(
                "harnessFailure",
                "fail offline",
                ExecuteOption.of().conversationId("conversation-harness-failure"));

        assertFalse(response.isSuccess());
        assertNotNull(response.getCause());
        assertTrue(causeMessages(response.getCause()).contains("offline model failure"),
                () -> "unexpected cause chain: " + causeMessages(response.getCause()));
    }

    private static String causeMessages(Throwable failure) {
        List<String> messages = new ArrayList<>();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            messages.add(current.getClass().getSimpleName() + ":" + current.getMessage());
        }
        return String.join(" -> ", messages);
    }
}

final class OfflineHarnessFixtures {

    static final RecordingStateStore STATE_STORE = new RecordingStateStore();
    static final OfflineSnapshotSpec SNAPSHOTS = new OfflineSnapshotSpec();
    static final OfflineSandboxClient SANDBOX_CLIENT = new OfflineSandboxClient();

    private OfflineHarnessFixtures() {
    }

    static void resetObservations() {
        STATE_STORE.resetObservations();
        SNAPSHOTS.reset();
        SANDBOX_CLIENT.reset();
    }
}

abstract class OfflineHarnessComponent extends HarnessAgentComponent {

    private final Model offlineModel;

    OfflineHarnessComponent(Model offlineModel) {
        this.offlineModel = offlineModel;
    }

    @Override
    protected final ModelSpec<?> model() {
        throw new AssertionError("the deterministic buildModel override must be used");
    }

    @Override
    protected final Model buildModel() {
        return offlineModel;
    }

    @Override
    protected final AgentStateStoreResolver stateStoreResolver() {
        return ignored -> new ResolvedAgentStateStore(OfflineHarnessFixtures.STATE_STORE, false);
    }

    @Override
    protected final SandboxClient<DockerSandboxClientOptions> dockerSandboxClient() {
        return OfflineHarnessFixtures.SANDBOX_CLIENT;
    }

    @Override
    protected final SandboxSnapshotProvider sandboxSnapshotProvider() {
        return OfflineHarnessComponent::snapshots;
    }

    private static SandboxSnapshotSpec snapshots(HarnessFilesystemContext ignored) {
        return OfflineHarnessFixtures.SNAPSHOTS;
    }

    @Override
    protected final PermissionContextState permissionContext() {
        return PermissionContextState.builder()
                .addAllowRule("execute", new PermissionRule(
                        "execute", null, PermissionBehavior.ALLOW, "offline-test"))
                .build();
    }

    @Override
    protected final HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
        return builder
                .disableSubagents()
                .disableCompaction()
                .disableToolResultEviction()
                .disableMemoryTools()
                .disableMemoryHooks()
                .disableWorkspaceContext()
                .disableAtPathExpansion()
                .disableDefaultWorkspaceSkills()
                .disableDynamicSkills()
                .disableToolsConfig()
                .disableFilesystemTools();
    }

    @Override
    protected final String systemPrompt() {
        return "Respond only through the deterministic offline test model.";
    }

    @Override
    protected final String userPrompt(LiteFlowAgentContext context) {
        Object request = context.getSlot().getChainReqData(context.getChainId());
        return request == null ? "" : request.toString();
    }

    @Override
    protected final void handleReply(Msg reply, LiteFlowAgentContext context) {
        context.getSlot().setOutput(getNodeId(), reply.getTextContent());
    }
}

@Component("harnessWriter")
final class HarnessWriterComponent extends OfflineHarnessComponent {

    HarnessWriterComponent() {
        super(new CommandModel(
                "harness-writer-tool",
                "write shared.txt shared-through-snapshot",
                null,
                "written"));
    }
}

@Component("harnessReaderA")
final class HarnessReaderAComponent extends OfflineHarnessComponent {

    HarnessReaderAComponent() {
        super(new CommandModel(
                "harness-reader-a-tool",
                "read shared.txt",
                "shared-through-snapshot",
                "shared-through-snapshot"));
    }
}

@Component("harnessReaderB")
final class HarnessReaderBComponent extends OfflineHarnessComponent {

    HarnessReaderBComponent() {
        super(new CommandModel(
                "harness-reader-b-tool",
                "read shared.txt",
                "shared-through-snapshot",
                "shared-through-snapshot"));
    }
}

@Component("harnessFailure")
final class HarnessFailureComponent extends OfflineHarnessComponent {

    HarnessFailureComponent() {
        super(new FailureModel());
    }
}

final class CommandModel implements Model {

    private final String toolId;
    private final String command;
    private final String expectedToolText;
    private final String fixedReply;

    CommandModel(
            String toolId, String command, String expectedToolText, String fixedReply) {
        this.toolId = toolId;
        this.command = command;
        this.expectedToolText = expectedToolText;
        this.fixedReply = fixedReply;
    }

    @Override
    public Flux<ChatResponse> stream(
            List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        Optional<ToolResultBlock> result = messages.stream()
                .flatMap(message -> message.getContentBlocks(ToolResultBlock.class).stream())
                .reduce((first, second) -> second);
        if (result.isEmpty()) {
            if (tools.stream().noneMatch(tool -> "execute".equals(tool.getName()))) {
                return Flux.error(new AssertionError("Harness execute tool is missing"));
            }
            ToolUseBlock use = new ToolUseBlock(
                    toolId,
                    "execute",
                    Map.of("command", command),
                    "{\"command\":\"" + command + "\"}",
                    Map.of(),
                    ToolCallState.PENDING);
            return Flux.just(response(use, "tool_calls"));
        }
        String toolText = result.orElseThrow().getOutput().stream()
                .filter(TextBlock.class::isInstance)
                .map(TextBlock.class::cast)
                .map(TextBlock::getText)
                .collect(Collectors.joining());
        if (expectedToolText != null && !toolText.contains(expectedToolText)) {
            return Flux.error(new AssertionError(
                    "unexpected offline tool result: " + toolText));
        }
        String reply = fixedReply == null ? toolText.strip() : fixedReply;
        return Flux.just(response(TextBlock.builder().text(reply).build(), "stop"));
    }

    @Override
    public String getModelName() {
        return "offline-command-model";
    }

    private static ChatResponse response(ContentBlock content, String finishReason) {
        return ChatResponse.builder()
                .content(List.of(content))
                .finishReason(finishReason)
                .build();
    }
}

final class FailureModel implements Model {

    @Override
    public Flux<ChatResponse> stream(
            List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        return Flux.error(new IllegalStateException("offline model failure"));
    }

    @Override
    public String getModelName() {
        return "offline-failure-model";
    }
}

final class RecordingStateStore extends InMemoryAgentStateStore {

    private final List<StateCall> saves = new CopyOnWriteArrayList<>();

    @Override
    public void save(String userId, String sessionId, String key, State value) {
        saves.add(new StateCall(sessionId, key));
        super.save(userId, sessionId, key, value);
    }

    Set<String> sessionsFor(String key) {
        return saves.stream()
                .filter(call -> key.equals(call.key()))
                .map(StateCall::sessionId)
                .collect(Collectors.toSet());
    }

    void resetObservations() {
        saves.clear();
    }

    private record StateCall(String sessionId, String key) {
    }
}

final class OfflineSandboxClient implements SandboxClient<DockerSandboxClientOptions> {

    private final AtomicInteger nextId = new AtomicInteger();
    private final AtomicInteger creates = new AtomicInteger();
    private final AtomicInteger resumes = new AtomicInteger();
    private final Map<String, DockerSandboxState> serialized = new ConcurrentHashMap<>();

    @Override
    public Sandbox create(
            WorkspaceSpec workspaceSpec,
            SandboxSnapshotSpec snapshotSpec,
            DockerSandboxClientOptions options) {
        creates.incrementAndGet();
        String sessionId = "offline-sandbox-" + nextId.incrementAndGet();
        DockerSandboxState state = new DockerSandboxState();
        state.setSessionId(sessionId);
        state.setWorkspaceSpec(workspaceSpec);
        state.setWorkspaceRoot(options.getWorkspaceRoot());
        state.setImage(options.getImage());
        state.setWorkspaceRootReady(false);
        if (snapshotSpec != null) {
            state.setSnapshot(snapshotSpec.build(sessionId));
        }
        return new OfflineSandbox(state);
    }

    @Override
    public Sandbox resume(SandboxState state) {
        resumes.incrementAndGet();
        return new OfflineSandbox((DockerSandboxState) state);
    }

    @Override
    public void delete(Sandbox sandbox) {
    }

    @Override
    public String serializeState(SandboxState state) {
        serialized.put(state.getSessionId(), (DockerSandboxState) state);
        return state.getSessionId();
    }

    @Override
    public SandboxState deserializeState(String json) {
        DockerSandboxState state = serialized.get(json);
        if (state == null) {
            throw new IllegalStateException("unknown offline sandbox state " + json);
        }
        return state;
    }

    @Override
    public SandboxState deserializeState(String json, SandboxSnapshotSpec snapshotSpec) {
        DockerSandboxState state = (DockerSandboxState) deserializeState(json);
        if (snapshotSpec != null) {
            state.setSnapshot(snapshotSpec.build(state.getSessionId()));
        }
        return state;
    }

    int createCount() {
        return creates.get();
    }

    int resumeCount() {
        return resumes.get();
    }

    void reset() {
        creates.set(0);
        resumes.set(0);
        serialized.clear();
    }
}

final class OfflineSandbox implements Sandbox {

    private final DockerSandboxState state;
    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private volatile boolean running;

    OfflineSandbox(DockerSandboxState state) {
        this.state = state;
    }

    @Override
    public void start() throws Exception {
        SandboxSnapshot snapshot = state.getSnapshot();
        if (snapshot != null && snapshot.isRestorable()) {
            try (InputStream input = snapshot.restore()) {
                hydrateWorkspace(input);
            }
        }
        state.setWorkspaceRootReady(true);
        running = true;
    }

    @Override
    public void stop() throws Exception {
        SandboxSnapshot snapshot = state.getSnapshot();
        if (snapshot != null && snapshot.isPersistenceEnabled()) {
            try (InputStream archive = persistWorkspace()) {
                snapshot.persist(archive);
            }
        }
        running = false;
    }

    @Override
    public void shutdown() {
        running = false;
        files.clear();
    }

    @Override
    public void close() throws Exception {
        try {
            stop();
        }
        finally {
            shutdown();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public SandboxState getState() {
        return state;
    }

    @Override
    public ExecResult exec(RuntimeContext context, String command, Integer timeoutSeconds) {
        if (command.startsWith("write ")) {
            String[] parts = command.split(" ", 3);
            files.put(parts[1], parts[2].getBytes(StandardCharsets.UTF_8));
            return new ExecResult(0, "written", "", false);
        }
        if (command.startsWith("read ")) {
            byte[] content = files.get(command.substring("read ".length()));
            return content == null
                    ? new ExecResult(1, "", "not found", false)
                    : new ExecResult(0, new String(content, StandardCharsets.UTF_8), "", false);
        }
        return new ExecResult(2, "", "unsupported offline command", false);
    }

    @Override
    public InputStream persistWorkspace() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            List<Map.Entry<String, byte[]>> entries = files.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .toList();
            output.writeInt(entries.size());
            for (Map.Entry<String, byte[]> entry : entries) {
                output.writeUTF(entry.getKey());
                output.writeInt(entry.getValue().length);
                output.write(entry.getValue());
            }
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    @Override
    public void hydrateWorkspace(InputStream archive) throws Exception {
        files.clear();
        try (DataInputStream input = new DataInputStream(archive)) {
            int count = input.readInt();
            for (int index = 0; index < count; index++) {
                String path = input.readUTF();
                files.put(path, input.readNBytes(input.readInt()));
            }
        }
    }
}

final class OfflineSnapshotSpec implements SandboxSnapshotSpec {

    private final Map<String, byte[]> archives = new ConcurrentHashMap<>();
    private final AtomicInteger restores = new AtomicInteger();

    @Override
    public SandboxSnapshot build(String snapshotId) {
        return new SandboxSnapshot() {
            @Override
            public void persist(InputStream workspaceArchive) throws Exception {
                archives.put(snapshotId, workspaceArchive.readAllBytes());
            }

            @Override
            public InputStream restore() {
                byte[] archive = archives.get(snapshotId);
                if (archive == null) {
                    throw new IllegalStateException("snapshot is not restorable: " + snapshotId);
                }
                restores.incrementAndGet();
                return new ByteArrayInputStream(archive);
            }

            @Override
            public boolean isRestorable() {
                return archives.containsKey(snapshotId);
            }

            @Override
            public String getId() {
                return snapshotId;
            }

            @Override
            public String getType() {
                return "offline";
            }
        };
    }

    int restoreCount() {
        return restores.get();
    }

    void reset() {
        archives.clear();
        restores.set(0);
    }
}
