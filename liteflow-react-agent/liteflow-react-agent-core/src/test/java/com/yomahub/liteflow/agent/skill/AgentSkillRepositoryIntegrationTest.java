package com.yomahub.liteflow.agent.skill;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.middleware.SkillTrackingMiddleware;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.ReActAgentRuntime;
import com.yomahub.liteflow.agent.testsupport.AgentTestContexts;
import com.yomahub.liteflow.agent.testsupport.ScriptedChatModel;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.model.Model;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.DynamicSkillMiddleware;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSkillRepositoryIntegrationTest {

    private static final String AGENT_NAMESPACE =
            "lf-cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";

    @Test
    void repositoriesLayerInOrderAndRuntimeContextSkillFilterIsAppliedPerInvocation() {
        AgentSkill lowShared = skill("shared", "low-priority-description", null);
        AgentSkill allowed = skill("allowed", "allowed-description", null);
        AgentSkill highShared = skill("shared", "high-priority-description", null);
        AgentSkill blocked = skill("blocked", "blocked-description", null);
        InMemoryRepository low = new InMemoryRepository("low", List.of(lowShared, allowed));
        InMemoryRepository high = new InMemoryRepository("high", List.of(highShared, blocked));
        TestComponent component = new TestComponent(List.of(low, high), List.of());
        component.filter = SkillFilter.only(
                highShared.getSkillId(), allowed.getSkillId());

        ReActAgentRuntime runtime = component.runtime(config());
        try {
            DynamicSkillMiddleware dynamic = middleware(runtime, DynamicSkillMiddleware.class);
            LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
            RuntimeContext allowedContext = AgentTestContexts.runtimeContext(invocation);
            String allowedPrompt = dynamic.onSystemPrompt(
                    runtime.agent(), allowedContext, "base").block();
            assertNotNull(allowedPrompt);
            assertTrue(allowedPrompt.contains("high-priority-description"), allowedPrompt);
            assertFalse(allowedPrompt.contains("low-priority-description"));
            assertTrue(allowedPrompt.contains("allowed-description"));
            assertFalse(allowedPrompt.contains("blocked-description"));

            RuntimeContext filteredContext = RuntimeContext.builder()
                    .userId(invocation.getRuntimeUserId())
                    .sessionId(invocation.getRuntimeSessionId())
                    .put(LiteFlowAgentContext.class, invocation)
                    .put(SkillFilter.class, SkillFilter.disable(allowed.getSkillId()))
                    .build();
            String filteredPrompt = dynamic.onSystemPrompt(
                    runtime.agent(), filteredContext, "base").block();
            assertNotNull(filteredPrompt);
            assertTrue(filteredPrompt.contains("high-priority-description"));
            assertFalse(filteredPrompt.contains("allowed-description"));
        } finally {
            runtime.close();
        }
    }

    @Test
    void successfulRealLoadToolResultIsTrackedInTheCurrentLiteFlowInvocation() {
        AgentSkill skill = skill("shared", "tracked-description", null);
        InMemoryRepository repository = new InMemoryRepository("tracked", List.of(skill));
        TestComponent component = new TestComponent(List.of(repository), List.of());

        ReActAgentRuntime runtime = component.runtime(config());
        try {
            DynamicSkillMiddleware dynamic = middleware(runtime, DynamicSkillMiddleware.class);
            SkillTrackingMiddleware tracking = middleware(runtime, SkillTrackingMiddleware.class);
            LiteFlowAgentContext invocation = AgentTestContexts.liteFlowContext();
            RuntimeContext runtimeContext = AgentTestContexts.runtimeContext(invocation);
            dynamic.onSystemPrompt(runtime.agent(), runtimeContext, "base").block();
            ToolUseBlock load = new ToolUseBlock(
                    "load-1",
                    SkillTrackingMiddleware.LOAD_SKILL_TOOL_NAME,
                    Map.of("skillId", skill.getSkillId(), "path", "SKILL.md"),
                    "{\"skillId\":\"" + skill.getSkillId() + "\",\"path\":\"SKILL.md\"}",
                    Map.of());

            tracking.onActing(
                            runtime.agent(),
                            runtimeContext,
                            new ActingInput(List.of(load)),
                            ignored -> runtime.agent().getToolkit()
                                    .callTools(
                                            List.of(load),
                                            null,
                                            runtime.agent(),
                                            runtimeContext)
                                    .flatMapMany(results -> Flux.fromIterable(results)
                                            .map(result -> {
                                                assertTrue(
                                                        result.getState()
                                                                != io.agentscope.core.message.ToolResultState.ERROR,
                                                        result.getOutput().toString());
                                                return successEvent(load);
                                            })))
                    .blockLast();

            assertEquals(List.of(skill.getSkillId()), invocation.getUsedSkills());
        } finally {
            runtime.close();
        }
    }

    @Test
    void repositoriesAreEnumeratedOnlyAtInvocationTime() {
        AgentSkill skill = skill("deferred", "deferred-description", null);
        DeferredRepository repository = new DeferredRepository(skill);
        TestComponent component = new TestComponent(List.of(repository), List.of());

        ReActAgentRuntime runtime = component.runtime(config());
        try {
            assertEquals(0, repository.enumerationCount.get());
            repository.open = true;
            DynamicSkillMiddleware dynamic = middleware(runtime, DynamicSkillMiddleware.class);
            String prompt = dynamic.onSystemPrompt(
                            runtime.agent(),
                            AgentTestContexts.runtimeContext(AgentTestContexts.liteFlowContext()),
                            "base")
                    .block();

            assertNotNull(prompt);
            assertTrue(prompt.contains("deferred-description"), prompt);
            assertEquals(1, repository.enumerationCount.get());
        } finally {
            runtime.close();
        }
    }

    @Test
    void coreNeverAdvertisesSkillCodeExecutionAndShellRemainsDisabled() {
        AgentSkill diskSkill = skill(
                "disk-skill", "disk-description", Path.of("/trusted/skill/source"));
        InMemoryRepository repository = new InMemoryRepository("disk", List.of(diskSkill));
        TestComponent component = new TestComponent(List.of(repository), List.of());

        ReActAgentRuntime runtime = component.runtime(config());
        try {
            DynamicSkillMiddleware dynamic = middleware(runtime, DynamicSkillMiddleware.class);
            assertEquals(1, runtime.agent().getMiddlewares().stream()
                    .filter(DynamicSkillMiddleware.class::isInstance)
                    .count());
            String prompt = dynamic.onSystemPrompt(
                            runtime.agent(),
                            AgentTestContexts.runtimeContext(AgentTestContexts.liteFlowContext()),
                            "base")
                    .block();
            assertNotNull(prompt);
            assertFalse(prompt.contains("execute_shell_command"));
            assertFalse(runtime.agent().getToolkit().getToolNames()
                    .contains("execute_shell_command"));
        } finally {
            runtime.close();
        }
    }

    @Test
    void runtimeClosesOwnedRepositoriesOnceAndNeverClosesBorrowedRepositories() {
        InMemoryRepository owned = new InMemoryRepository(
                "owned", List.of(skill("owned-skill", "owned-description", null)));
        InMemoryRepository borrowed = new InMemoryRepository(
                "borrowed", List.of(skill("borrowed-skill", "borrowed-description", null)));
        TestComponent component = new TestComponent(
                List.of(owned, borrowed, owned), List.of(owned));

        ReActAgentRuntime runtime = component.runtime(config());
        runtime.close();
        runtime.close();

        assertEquals(1, owned.closeCount.get());
        assertEquals(0, borrowed.closeCount.get());
    }

    @Test
    void customizedAgentMustRetainNativeDynamicSkillMiddlewareAndBuildFailureClosesOwnedRepo() {
        InMemoryRepository owned = new InMemoryRepository(
                "owned", List.of(skill("owned-skill", "owned-description", null)));
        TestComponent component = new TestComponent(List.of(owned), List.of(owned));
        component.customizer = builder -> {
            ReActAgent snapshot = builder.build();
            try {
                List<MiddlewareBase> withoutDynamic = snapshot.getMiddlewares().stream()
                        .filter(middleware -> !(middleware instanceof DynamicSkillMiddleware))
                        .toList();
                return ReActAgent.Builder.fromAgent(snapshot)
                        .defaultSessionId(snapshot.getDefaultSessionId())
                        .stateStore(snapshot.getStateStore())
                        .middlewares(withoutDynamic);
            } finally {
                snapshot.close();
            }
        };

        AgentConfigException failure = assertThrows(
                AgentConfigException.class, () -> component.runtime(config()));

        assertTrue(failure.getMessage().contains("DynamicSkillMiddleware"));
        assertEquals(1, owned.closeCount.get());
    }

    @Test
    void customizerCannotEnableASecondCodeExecutingDynamicSkillMiddleware() {
        InMemoryRepository repository = new InMemoryRepository(
                "secured", List.of(skill("secured-skill", "secured-description", null)));
        TestComponent component = new TestComponent(List.of(repository), List.of());
        component.customizer = builder -> builder
                .dynamicSkillsEnabled(true)
                .skillCodeExecutionEnabled(true);

        AgentConfigException failure = assertThrows(
                AgentConfigException.class, () -> component.runtime(config()));

        assertTrue(failure.getMessage().contains("DynamicSkillMiddleware"));
    }

    @Test
    void customizerCannotReplaceManagedDynamicSkillMiddlewareWithAnEmptyOne() {
        InMemoryRepository repository = new InMemoryRepository(
                "secured", List.of(skill("secured-skill", "secured-description", null)));
        TestComponent component = new TestComponent(List.of(repository), List.of());
        component.customizer = builder -> builder
                .middleware(new DynamicSkillMiddleware(List.of(), new io.agentscope.core.tool.Toolkit()))
                .dynamicSkillsEnabled(false);

        AgentConfigException failure = assertThrows(
                AgentConfigException.class, () -> component.runtime(config()));

        assertTrue(failure.getMessage().contains("DynamicSkillMiddleware"));
    }

    private static ToolResultEndEvent successEvent(ToolUseBlock toolUse) {
        return new ToolResultEndEvent(
                "reply-1", toolUse.getId(), toolUse.getName(),
                io.agentscope.core.message.ToolResultState.SUCCESS);
    }

    private static AgentSkill skill(String name, String description, Path originDir) {
        return new AgentSkill(
                Map.of("name", name, "description", description),
                "instructions for " + name,
                Map.of(),
                "memory",
                originDir);
    }

    private static <T> T middleware(ReActAgentRuntime runtime, Class<T> type) {
        return runtime.agent().getMiddlewares().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow();
    }

    private static AgentConfig config() {
        AgentConfig config = new AgentConfig();
        config.getRuntime().setNamespace("skill-test");
        config.getRuntime().setTimeout(Duration.ofSeconds(2));
        return config;
    }

    private static final class TestComponent extends ReActAgentComponent {
        private final Model model = new ScriptedChatModel("reply");
        private final List<AgentSkillRepository> repositories;
        private final List<AgentSkillRepository> owned;
        private SkillFilter filter = SkillFilter.all();
        private UnaryOperator<ReActAgent.Builder> customizer = UnaryOperator.identity();

        private TestComponent(
                List<AgentSkillRepository> repositories,
                List<AgentSkillRepository> owned) {
            this.repositories = repositories;
            this.owned = owned;
        }

        private ReActAgentRuntime runtime(AgentConfig config) {
            return buildRuntime(new AgentRuntimeBuildContext(
                    config, "skill-agent", "agent-key", AGENT_NAMESPACE));
        }

        @Override
        protected ModelSpec<?> model() {
            throw new AssertionError("buildModel override must be used");
        }

        @Override
        protected Model buildModel() {
            return model;
        }

        @Override
        protected String systemPrompt() {
            return "skill test";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "hello";
        }

        @Override
        protected List<AgentSkillRepository> skillRepositories() {
            return repositories;
        }

        @Override
        protected boolean ownsSkillRepository(AgentSkillRepository repository) {
            return owned.stream().anyMatch(candidate -> candidate == repository);
        }

        @Override
        protected SkillFilter skillFilter() {
            return filter;
        }

        @Override
        protected ReActAgent.Builder customizeAgent(ReActAgent.Builder builder) {
            return customizer.apply(builder);
        }
    }

    private static final class InMemoryRepository implements AgentSkillRepository {
        private final String name;
        private final List<AgentSkill> skills;
        private final AtomicInteger closeCount = new AtomicInteger();
        private boolean writable;

        private InMemoryRepository(String name, List<AgentSkill> skills) {
            this.name = name;
            this.skills = new ArrayList<>(skills);
        }

        @Override
        public AgentSkill getSkill(String name) {
            return skills.stream()
                    .filter(skill -> skill.getName().equals(name))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<String> getAllSkillNames() {
            return skills.stream().map(AgentSkill::getSkillId).toList();
        }

        @Override
        public List<AgentSkill> getAllSkills() {
            return List.copyOf(skills);
        }

        @Override
        public boolean save(List<AgentSkill> skills, boolean force) {
            this.skills.addAll(skills);
            return !skills.isEmpty();
        }

        @Override
        public boolean delete(String skillName) {
            return skills.removeIf(skill -> skill.getName().equals(skillName));
        }

        @Override
        public boolean skillExists(String skillName) {
            return skills.stream().anyMatch(skill -> skill.getName().equals(skillName));
        }

        @Override
        public AgentSkillRepositoryInfo getRepositoryInfo() {
            return new AgentSkillRepositoryInfo("memory", name, writable);
        }

        @Override
        public String getSource() {
            return "memory_" + name;
        }

        @Override
        public void setWriteable(boolean writeable) {
            writable = writeable;
        }

        @Override
        public boolean isWriteable() {
            return writable;
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    private static final class DeferredRepository implements AgentSkillRepository {
        private final AgentSkill skill;
        private final AtomicInteger enumerationCount = new AtomicInteger();
        private volatile boolean open;

        private DeferredRepository(AgentSkill skill) {
            this.skill = skill;
        }

        @Override
        public AgentSkill getSkill(String name) {
            return skill.getName().equals(name) ? skill : null;
        }

        @Override
        public List<String> getAllSkillNames() {
            return List.of(skill.getSkillId());
        }

        @Override
        public List<AgentSkill> getAllSkills() {
            enumerationCount.incrementAndGet();
            if (!open) {
                throw new IllegalStateException("repository is not available during build");
            }
            return List.of(skill);
        }

        @Override
        public boolean save(List<AgentSkill> skills, boolean force) {
            return false;
        }

        @Override
        public boolean delete(String skillName) {
            return false;
        }

        @Override
        public boolean skillExists(String skillName) {
            return skill.getName().equals(skillName);
        }

        @Override
        public AgentSkillRepositoryInfo getRepositoryInfo() {
            return new AgentSkillRepositoryInfo("memory", "deferred", false);
        }

        @Override
        public String getSource() {
            return "memory_deferred";
        }

        @Override
        public void setWriteable(boolean writeable) {
        }

        @Override
        public boolean isWriteable() {
            return false;
        }
    }
}
