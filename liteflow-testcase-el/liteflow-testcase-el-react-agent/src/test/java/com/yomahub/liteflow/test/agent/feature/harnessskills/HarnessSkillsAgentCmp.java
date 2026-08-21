package com.yomahub.liteflow.test.agent.feature.harnessskills;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.SkillRepositoryRegistration;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component("harnessSkillsAgent")
public class HarnessSkillsAgentCmp extends HarnessAgentComponent {

    private static final AtomicReference<String> ALLOWED_SKILL_ID = new AtomicReference<>();
    private static final AtomicReference<List<String>> USED_SKILLS = new AtomicReference<>(List.of());
    private static final AtomicReference<SkillLoadingModel> MODEL = new AtomicReference<>();
    static void reset() {
        ALLOWED_SKILL_ID.set(null);
        USED_SKILLS.set(List.of());
        MODEL.set(null);
    }

    static String renderedPrompt() {
        SkillLoadingModel model = MODEL.get();
        return model == null ? "" : model.renderedText();
    }

    static String allowedSkillId() {
        return ALLOWED_SKILL_ID.get();
    }

    static List<String> usedSkills() {
        return USED_SKILLS.get();
    }

    static int successfulLoadResults() {
        SkillLoadingModel model = MODEL.get();
        return model == null ? 0 : model.successfulLoadResults();
    }

    @Override
    protected ModelSpec<?> model() {
        throw new AssertionError("offline buildModel must be used");
    }

    @Override
    protected Model buildModel() {
        AgentSkill allowed = loadAllowedSkill();
        ALLOWED_SKILL_ID.set(allowed.getSkillId());
        SkillLoadingModel model = new SkillLoadingModel(allowed);
        MODEL.set(model);
        return model;
    }

    @Override
    protected List<SkillRepositoryRegistration> skillRepositoryRegistrations() {
        return List.of(SkillRepositoryRegistration.owned(
                new FileSystemSkillRepository(skillsRoot(), false)));
    }

    @Override
    protected SkillFilter skillFilter() {
        return SkillFilter.only("demo");
    }

    @Override
    protected PermissionContextState permissionContext() {
        return PermissionContextState.builder()
                .addAllowRule("load_skill_through_path", new PermissionRule(
                        "load_skill_through_path", null,
                        PermissionBehavior.ALLOW, "offline skill test"))
                .build();
    }

    @Override
    protected String systemPrompt() {
        return "Use only visible deterministic skills.";
    }

    @Override
    protected String userPrompt(LiteFlowAgentContext context) {
        return "load demo";
    }

    @Override
    protected void handleReply(Msg reply, LiteFlowAgentContext context) {
        USED_SKILLS.set(context.getUsedSkills());
        super.handleReply(reply, context);
    }

    private AgentSkill loadAllowedSkill() {
        try (FileSystemSkillRepository repository =
                     new FileSystemSkillRepository(skillsRoot(), false)) {
            return repository.getAllSkills().stream()
                    .filter(skill -> "demo".equals(skill.getName()))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private Path skillsRoot() {
        return Path.of(agentConfig().getSkills().getPath());
    }
}

final class SkillLoadingModel implements Model {

    private final AgentSkill skill;
    private final AtomicInteger calls = new AtomicInteger();
    private final List<List<Msg>> inputs = new CopyOnWriteArrayList<>();

    SkillLoadingModel(AgentSkill skill) {
        this.skill = skill;
    }

    @Override
    public Flux<ChatResponse> stream(
            List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        inputs.add(List.copyOf(messages));
        if (calls.getAndIncrement() == 0) {
            if (tools.stream().noneMatch(tool ->
                    "load_skill_through_path".equals(tool.getName()))) {
                return Flux.error(new AssertionError("dynamic skill tool is missing"));
            }
            ToolUseBlock load = new ToolUseBlock(
                    "load-demo",
                    "load_skill_through_path",
                    Map.of("skillId", skill.getSkillId(), "path", "SKILL.md"),
                    "{\"skillId\":\"" + skill.getSkillId()
                            + "\",\"path\":\"SKILL.md\"}",
                    Map.of(),
                    ToolCallState.PENDING);
            return Flux.just(response(load, "tool_calls"));
        }
        return Flux.just(response(TextBlock.builder().text("skill-loaded").build(), "stop"));
    }

    @Override
    public String getModelName() {
        return "offline-harness-skill-model";
    }

    String renderedText() {
        return inputs.stream()
                .flatMap(Collection::stream)
                .flatMap(message -> message.getContent().stream())
                .filter(TextBlock.class::isInstance)
                .map(TextBlock.class::cast)
                .map(TextBlock::getText)
                .reduce("", (left, right) -> left + "\n" + right);
    }

    int successfulLoadResults() {
        return (int) inputs.stream()
                .flatMap(Collection::stream)
                .flatMap(message -> message.getContentBlocks(ToolResultBlock.class).stream())
                .filter(result -> "load_skill_through_path".equals(result.getName()))
                .filter(result -> result.getState() == ToolResultState.SUCCESS)
                .count();
    }

    private static ChatResponse response(ContentBlock block, String reason) {
        return ChatResponse.builder().content(List.of(block)).finishReason(reason).build();
    }
}
