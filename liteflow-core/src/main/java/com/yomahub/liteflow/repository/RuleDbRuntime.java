package com.yomahub.liteflow.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ChainLoadException;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.RuleDbConfig;
import com.yomahub.liteflow.repository.runtime.ChainCandidateLoader;
import com.yomahub.liteflow.repository.runtime.RuleTargetState;
import com.yomahub.liteflow.repository.runtime.RuleTargetStatus;
import com.yomahub.liteflow.repository.runtime.ScriptCandidateLoader;
import com.yomahub.liteflow.repository.vo.ChangeRecord;
import com.yomahub.liteflow.repository.vo.ChainMeta;
import com.yomahub.liteflow.repository.vo.ChainRecord;
import com.yomahub.liteflow.repository.vo.RuleManifest;
import com.yomahub.liteflow.repository.vo.RuleDbRuntimeSnapshot;
import com.yomahub.liteflow.repository.vo.ScriptMeta;
import com.yomahub.liteflow.repository.vo.ScriptRecord;
import com.yomahub.liteflow.util.ElFingerprint;
import com.yomahub.liteflow.script.ScriptExecutorFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rule-DB 模式运行时：常驻版本戳索引 + 懒回源。后台变更同步/对账在 Task 5 补入，
 * 有界缓存在 Task 4 补入。
 *
 * @author Bryan.Zhang
 * @since 2.16.1
 */
public class RuleDbRuntime {

	private static final LFLog LOG = LFLoggerManager.getLogger(RuleDbRuntime.class);
	private static final int MAX_FAILED_TARGETS = 20;

	/** Per-target desired and active metadata retained independently of the execution cache. */
	private static final ConcurrentHashMap<String, RuleTargetState> CHAIN_STATES = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, RuleTargetState> SCRIPT_STATES = new ConcurrentHashMap<>();

	/** Shadow objects registered by this runtime, used to avoid removing application-owned metadata. */
	private static final Map<String, Chain> SHADOW_CHAINS = new ConcurrentHashMap<>();
	private static final Map<String, Node> SHADOW_SCRIPTS = new ConcurrentHashMap<>();
	private static final Map<Chain, RuleLoadAttempt> CHAIN_LOAD_STATES = new ConcurrentHashMap<>();
	private static final Map<Node, RuleLoadAttempt> SCRIPT_LOAD_STATES = new ConcurrentHashMap<>();
	private static final Map<String, ScriptArtifact> SCRIPT_ARTIFACTS = new ConcurrentHashMap<>();

	private static final class ScriptArtifact {

		private final RuleTargetState state;
		private final String id;
		private final String language;
		private int users;
		private boolean retired;

		private ScriptArtifact(RuleTargetState state, String id, String language) {
			this.state = state;
			this.id = id;
			this.language = language;
		}
	}

	public static final class ScriptExecutionLease implements AutoCloseable {

		private ScriptArtifact artifact;

		private ScriptExecutionLease(ScriptArtifact artifact) {
			this.artifact = artifact;
		}

		public String getArtifactId() {
			return artifact.id;
		}

		public String getLanguage() {
			return artifact.language;
		}

		@Override
		public void close() {
			ScriptArtifact current = artifact;
			if (current != null) {
				artifact = null;
				releaseScriptExecution(current);
			}
		}
	}

	private static final class RuleLoadAttempt {

		private final RuleTargetState state;
		private final long version;
		private final String md5;

		private RuleLoadAttempt(RuleTargetState state) {
			synchronized (state) {
				this.state = state;
				this.version = state.getDesiredVersion();
				this.md5 = state.getDesiredMd5();
			}
		}

		private RuleLoadAttempt(RuleTargetState state, long version, String md5) {
			this.state = state;
			this.version = version;
			this.md5 = md5;
		}

		private boolean markFailedIfCurrent(RuleTargetState current, Throwable error) {
			if (current != state) {
				return false;
			}
			synchronized (state) {
				return state.isGenerationCurrent(version, md5) && state.markFailed(error);
			}
		}
	}

	static final AtomicLong LAST_APPLIED_SEQ = new AtomicLong(0);

	private static volatile boolean initialized = false;
	private static volatile RuleRepository activeRepository;

	/** Cached because provider discovery and enabled configuration are stable during one runtime lifecycle. */
	private static volatile Boolean activeFlag;

	public static boolean isActive() {
		Boolean cached = activeFlag;
		if (cached != null) {
			return cached;
		}
		LiteflowConfig config = LiteflowConfigGetter.get();
		RuleDbConfig ruleDb = config.getRuleDb();
		if (ruleDb != null && Boolean.FALSE.equals(ruleDb.getEnabled())) {
			activeFlag = Boolean.FALSE;
			return false;
		}
		if (!RuleDbProviderHolder.hasImplementation()) {
			activeFlag = Boolean.FALSE;
			return false;
		}
		// 未显式配置 ruleDb 但 classpath 有实现，也视为激活（零配置理念）
		activeFlag = Boolean.TRUE;
		return true;
	}

	public static synchronized void init() {
		if (initialized || !isActive()) {
			return;
		}
		RuleDbProvider provider = RuleDbProviderHolder.get();
		if (provider == null) {
			return;
		}

		try {
			// Open the source before reading the manifest so events observed during the
			// snapshot are buffered and replayed after its sequence baseline is known.
			RuleDbSyncManager.open(provider);
			activeRepository = RuleDbSyncManager.activeRepository();
			RuleManifest manifest = activeRepository.fetchManifest();
			validateManifest(manifest);
			validateManifestOwnership(manifest);

			// 注册 chain 影子
			if (CollUtil.isNotEmpty(manifest.getChains())) {
				for (ChainMeta cm : manifest.getChains()) {
					registerShadowChain(cm);
				}
			}
			// 注册 script 影子
			if (CollUtil.isNotEmpty(manifest.getScripts())) {
				for (ScriptMeta sm : manifest.getScripts()) {
					registerShadowScript(sm);
				}
			}
			initialized = true;

			// 初始化有界缓存（容量按 chain 条数）
			int capacity = 500;
			RuleDbConfig cacheCfg = LiteflowConfigGetter.get().getRuleDb();
			if (cacheCfg != null && cacheCfg.getCache() != null && cacheCfg.getCache().getCapacity() != null) {
				capacity = cacheCfg.getCache().getCapacity();
			}
			RuleDbCache.init(capacity);

			// Activate buffered changes at the manifest baseline, then reconcile and preload.
			RuleDbSyncManager.activate(manifest.getLatestSeq());
			RuleDbSyncManager.startReconcileScheduler();
			preload();
		} catch (RuntimeException e) {
			RuleDbSyncManager.stop();
			clearRuntimeState();
			throw e;
		}
	}

	private static void preload() {
		RuleDbConfig ruleDb = LiteflowConfigGetter.get().getRuleDb();
		if (ruleDb == null || ruleDb.getCache() == null
				|| StrUtil.isBlank(ruleDb.getCache().getPreloadChainIds())) {
			return;
		}
		for (String chainId : ruleDb.getCache().getPreloadChainIds().split(",")) {
			String trimmed = chainId.trim();
			if (StrUtil.isNotBlank(trimmed) && isLive(CHAIN_STATES.get(trimmed))) {
				try {
					LiteFlowChainELBuilder.buildUnCompileChain(FlowBus.getChain(trimmed));
				} catch (Exception e) {
					LOG.warn("preload chain[{}] failed: {}", trimmed, e.getMessage());
				}
			}
		}
	}

	/**
	 * 注册脚本影子 Node：有元数据（type/language/name），无脚本源码，isCompiled=false。
	 * 直接 new Node 并放入 nodeMap，进入未编译态。
	 */
	private static void registerShadowScript(ScriptMeta sm) {
		if (FlowBus.containNode(sm.getNodeId())) {
			Node existing = FlowBus.getNode(sm.getNodeId());
			if (SHADOW_SCRIPTS.get(sm.getNodeId()) != existing) {
				throw collision("script", sm.getNodeId());
			}
		}
		NodeTypeEnum type = NodeTypeEnum.getEnumByCode(sm.getType());
		Node node = new Node(sm.getNodeId(), sm.getName(), type, null, sm.getLanguage());
		node.setCompiled(false);
		FlowBus.getNodeMap().put(sm.getNodeId(), node);
		SHADOW_SCRIPTS.put(sm.getNodeId(), node);
		SCRIPT_STATES.compute(sm.getNodeId(), (id, state) -> desiredState(state, sm.getVersion(), sm.getMd5()));
	}

	private static void registerShadowChain(ChainMeta cm) {
		String chainId = cm.getChainId();
		Chain existing = FlowBus.getChain(chainId);
		Chain owned = SHADOW_CHAINS.get(chainId);
		if (existing != null && existing != owned) {
			throw collision("chain", chainId);
		}
		if (existing == null) {
			FlowBus.addChain(chainId);
			existing = FlowBus.getChain(chainId);
		}
		if (existing != null) {
			SHADOW_CHAINS.put(chainId, existing);
			CHAIN_STATES.compute(chainId, (id, state) -> desiredState(state, cm.getVersion(), cm.getMd5()));
		}
	}

	private static RuleTargetState desiredState(RuleTargetState state, long version, String md5) {
		if (state == null || state.getStatus() == RuleTargetStatus.DELETED) {
			return new RuleTargetState(version, md5);
		}
		state.updateDesired(version, md5);
		return state;
	}

	private static boolean isLive(RuleTargetState state) {
		return state != null && state.getStatus() != RuleTargetStatus.DELETED;
	}

	/** Loads Rule-DB shadows whose route metadata is still unknown or stale. */
	public static void prepareRouteChains() {
		List<Chain> candidates = new ArrayList<>();
		synchronized (RuleDbRuntime.class) {
			for (Map.Entry<String, Chain> entry : SHADOW_CHAINS.entrySet()) {
				RuleTargetState state = CHAIN_STATES.get(entry.getKey());
				if (isLive(state) && state.getStatus() != RuleTargetStatus.READY) {
					candidates.add(entry.getValue());
				}
			}
		}
		for (Chain chain : candidates) {
			try {
				loadAndInstallChainCandidate(chain);
			}
			catch (RuntimeException e) {
				LOG.warn("prepare route chain[{}] failed: {}", chain.getChainId(), e.getMessage());
			}
		}
	}

	/** Builds a stable candidate off-bus and installs it only after compilation succeeds. */
	public static boolean loadAndInstallChainCandidate(Chain chain) {
		if (chain == null) {
			return false;
		}
		String chainId = chain.getChainId();
		RuleTargetState state = CHAIN_STATES.get(chainId);
		if (!isLive(state)) {
			return false;
		}
		if (chain != SHADOW_CHAINS.get(chainId) || FlowBus.getChain(chainId) != chain) {
			throw new ChainLoadException(StrUtil.format("chain[{}] is no longer the active rule-db shadow", chainId));
		}
		if (state.getStatus() == RuleTargetStatus.READY && state.getActiveVersion() > 0 && chain.isCompiled()) {
			return true;
		}

		boolean hadActive = state.getActiveVersion() > 0;
		if (hadActive && !state.getLoadLock().tryLock()) {
			return true;
		}
		if (!hadActive) {
			state.getLoadLock().lock();
		}
		try {
			RuleTargetState current = CHAIN_STATES.get(chainId);
			if (current != state || !isLive(current)
					|| chain != SHADOW_CHAINS.get(chainId) || FlowBus.getChain(chainId) != chain) {
				throw new ChainLoadException(StrUtil.format("chain[{}] changed or was deleted while loading", chainId));
			}
			if (state.getStatus() == RuleTargetStatus.READY && state.getActiveVersion() > 0 && chain.isCompiled()) {
				return true;
			}

			RuleLoadAttempt attempt = new RuleLoadAttempt(state);
			state.markLoading();
			try {
				ChainCandidateLoader.Candidate candidate = new ChainCandidateLoader(repositoryForRead(), retryTimes())
						.load(chainId, attempt.version, attempt.md5);
				installChainCandidate(chainId, chain, attempt, candidate);
				return true;
			} catch (RuntimeException e) {
				synchronized (RuleDbRuntime.class) {
					attempt.markFailedIfCurrent(CHAIN_STATES.get(chainId), e);
				}
				if (hadActive && CHAIN_STATES.get(chainId) == state && isLive(state)) {
					return true;
				}
				throw e;
			}
		} finally {
			state.getLoadLock().unlock();
		}
	}

	private static synchronized void installChainCandidate(String chainId, Chain chain, RuleLoadAttempt attempt,
			ChainCandidateLoader.Candidate candidate) {
		RuleTargetState state = CHAIN_STATES.get(chainId);
		if (state != attempt.state || !isLive(state)
				|| chain != SHADOW_CHAINS.get(chainId) || FlowBus.getChain(chainId) != chain
				|| !state.isGenerationCurrent(candidate.getVersion(), candidate.getMd5())
				|| !scriptGenerationsCurrent(candidate.getChain(),
						Collections.newSetFromMap(new IdentityHashMap<Executable, Boolean>()))
				|| !state.markLoaded(candidate.getVersion(), candidate.getMd5())) {
			throw new ChainLoadException(StrUtil.format("chain[{}] changed or was deleted while loading", chainId));
		}
		LiteFlowChainELBuilder.assignNodeInstanceIds(candidate.getChain(), chainId);
		chain.installCompiledRule(candidate.getChain());
		if (!state.activateLoaded()) {
			throw new ChainLoadException(StrUtil.format("chain[{}] changed or was deleted while activating", chainId));
		}
		recordChainCache(chainId);
	}

	/** buildUnCompileChain 回源钩子：影子或版本失效时，拉内容填 EL，交由后续既有编译逻辑 */
	public static void ensureChainLoaded(String chainId) {
		RuleTargetState state = CHAIN_STATES.get(chainId);
		if (!isLive(state)) {
			return; // 非 rule-db 管理的 chain（如手动 build），不干预
		}
		Chain chain = FlowBus.getChain(chainId);
		if (chain != null) {
			CHAIN_LOAD_STATES.put(chain, new RuleLoadAttempt(state));
		}
		if (chain != null && StrUtil.isNotBlank(chain.getEl())
				&& (state.getStatus() == RuleTargetStatus.READY || state.isDesiredLoaded())) {
			return; // EL 已在手且版本一致
		}
		ChainRecord record = fetchChainWithRetry(chainId);
		if (record == null || !record.isEnable()) {
			throw new ChainLoadException(StrUtil.format("chain[{}] not found or disabled in rule repository", chainId));
		}
		if (!state.isGenerationCurrent(record.getVersion(), record.getMd5())) {
			if (state.isDeleted() && chain != null) {
				SHADOW_CHAINS.remove(chainId, chain);
				restoreOrRemoveLateChain(chainId, chain);
			}
			throw new ChainLoadException(StrUtil.format("chain[{}] changed or was deleted while loading", chainId));
		}
		if (chain == null) {
			// 索引里有但 FlowBus 无影子（理论上不发生，防御性补注册）
			FlowBus.addChain(chainId);
			chain = FlowBus.getChain(chainId);
			if (chain != null) {
				SHADOW_CHAINS.put(chainId, chain);
			}
		}
		chain.setEl(record.getEl());
		// 与 builder、动态 EL 执行入口保持一致，按原文计算指纹
		chain.setElMd5(ElFingerprint.of(record.getEl()));
		chain.setRouteEl(record.getRoute());
		if (StrUtil.isNotBlank(record.getNamespace())) {
			chain.setNamespace(record.getNamespace());
		}
		chain.setCompiled(false);
		if (!state.markLoaded(record.getVersion(), record.getMd5())) {
			if (state.isDeleted() && chain != null) {
				SHADOW_CHAINS.remove(chainId, chain);
				restoreOrRemoveLateChain(chainId, chain);
			}
			throw new ChainLoadException(StrUtil.format("chain[{}] changed or was deleted while loading", chainId));
		}
		CHAIN_LOAD_STATES.put(chain, new RuleLoadAttempt(state, record.getVersion(), record.getMd5()));
	}

	/**
	 * compileScriptNode 回源钩子：脚本影子/失效时拉源码填入 node。
	 * 注意：EL 编译期 {@code OperatorHelper.convert} 会对 Node 做 clone，
	 * 故 compileScriptNode 收到的 node 往往是 nodeMap 中影子的副本——
	 * 必须把源码写到传入的 node 上，而非重新 FlowBus.getNode(nodeId)。
	 */
	public static void ensureScriptLoaded(Node node) {
		String nodeId = node.getId();
		RuleTargetState state = SCRIPT_STATES.get(nodeId);
		if (state == null) {
			return;
		}
		if (state.isDeleted()) {
			throw new ChainLoadException(StrUtil.format("script node[{}] was deleted from rule repository", nodeId));
		}
		SCRIPT_LOAD_STATES.put(node, new RuleLoadAttempt(state));
		if (StrUtil.isNotBlank(node.getScript())
				&& (state.getStatus() == RuleTargetStatus.READY || state.isDesiredLoaded())) {
			return;
		}
		ScriptRecord record = fetchScriptWithRetry(nodeId);
		if (record == null || !record.isEnable()) {
			throw new ChainLoadException(StrUtil.format("script node[{}] not found or disabled in rule repository", nodeId));
		}
		if (!state.isGenerationCurrent(record.getVersion(), record.getMd5())) {
			if (state.isDeleted()) {
				SHADOW_SCRIPTS.remove(nodeId, node);
				restoreOrRemoveLateScript(nodeId, node);
			}
			throw new ChainLoadException(StrUtil.format("script node[{}] changed or was deleted while loading", nodeId));
		}
		node.setScript(record.getScript());
		node.setLanguage(record.getLanguage());
		if (!state.markLoaded(record.getVersion(), record.getMd5())) {
			if (state.isDeleted()) {
				SHADOW_SCRIPTS.remove(nodeId, node);
				restoreOrRemoveLateScript(nodeId, node);
			}
			throw new ChainLoadException(StrUtil.format("script node[{}] changed or was deleted while loading", nodeId));
		}
		SCRIPT_LOAD_STATES.put(node, new RuleLoadAttempt(state, record.getVersion(), record.getMd5()));
	}

	public static void refreshScript(Node node) {
		String nodeId = node.getId();
		RuleTargetState state = SCRIPT_STATES.get(nodeId);
		if (state == null) {
			return;
		}
		if (state.isDeleted()) {
			throw new ChainLoadException(StrUtil.format("script node[{}] was deleted from rule repository", nodeId));
		}

		Node active = SHADOW_SCRIPTS.get(nodeId);
		if (state.getStatus() == RuleTargetStatus.READY && state.getActiveVersion() > 0
				&& active != null && active.isCompiled()) {
			if (node.getRuleDbScriptVersion() != state.getActiveVersion()) {
				node.installCompiledScript(active, state.getActiveVersion());
			}
			return;
		}

		boolean hadActive = state.getActiveVersion() > 0 && active != null && active.isCompiled();
		if (hadActive && !state.getLoadLock().tryLock()) {
			return;
		}
		if (!hadActive) {
			state.getLoadLock().lock();
		}
		try {
			RuleTargetState current = SCRIPT_STATES.get(nodeId);
			if (current != state || !isLive(current)) {
				throw new ChainLoadException(StrUtil.format("script node[{}] changed or was deleted while loading", nodeId));
			}
			active = SHADOW_SCRIPTS.get(nodeId);
			if (state.getStatus() == RuleTargetStatus.READY && state.getActiveVersion() > 0
					&& active != null && active.isCompiled()) {
				node.installCompiledScript(active, state.getActiveVersion());
				return;
			}

			RuleLoadAttempt attempt = new RuleLoadAttempt(state);
			state.markLoading();
			try (ScriptCandidateLoader.Candidate candidate =
						 new ScriptCandidateLoader(repositoryForRead(), retryTimes())
								 .load(nodeId, attempt.version, attempt.md5)) {
				installScriptCandidate(nodeId, node, attempt, candidate);
			}
			catch (RuntimeException e) {
				synchronized (RuleDbRuntime.class) {
					attempt.markFailedIfCurrent(SCRIPT_STATES.get(nodeId), e);
				}
				active = SHADOW_SCRIPTS.get(nodeId);
				if (hadActive && SCRIPT_STATES.get(nodeId) == state && isLive(state)
						&& active != null && active.isCompiled()) {
					node.installCompiledScript(active, state.getActiveVersion());
					return;
				}
				throw e;
			}
		}
		finally {
			state.getLoadLock().unlock();
		}
	}

	private static synchronized void installScriptCandidate(String nodeId, Node requested,
			RuleLoadAttempt attempt, ScriptCandidateLoader.Candidate candidate) {
		RuleTargetState state = SCRIPT_STATES.get(nodeId);
		ScriptRecord record = candidate.getRecord();
		if (state != attempt.state || !isLive(state)
				|| !state.isGenerationCurrent(record.getVersion(), record.getMd5())) {
			throw new ChainLoadException(StrUtil.format("script node[{}] changed or was deleted while loading", nodeId));
		}

		String activeArtifactId = nodeId + "@ruleDbActive@" + record.getVersion() + "@" + UUID.randomUUID();
		Node installed = new Node(nodeId, record.getName(), NodeTypeEnum.getEnumByCode(record.getType()),
				record.getScript(), record.getLanguage());
		ScriptArtifact retired = null;
		boolean hadActive = false;
		try {
			FlowBus.compileUnpublishedScriptNode(installed, activeArtifactId);
			installed.installCompiledScript(installed, record.getVersion());
			synchronized (state) {
				Node previous = SHADOW_SCRIPTS.get(nodeId);
				if (state != SCRIPT_STATES.get(nodeId) || !isLive(state) || previous == null
						|| FlowBus.getNode(nodeId) != previous
						|| !state.isGenerationCurrent(record.getVersion(), record.getMd5())
						|| !state.markLoaded(record.getVersion(), record.getMd5())) {
					throw new ChainLoadException(StrUtil.format("script node[{}] changed or was deleted while loading", nodeId));
				}

				hadActive = state.getActiveVersion() > 0 && previous.isCompiled();
				boolean flowReplaced = FlowBus.replaceNode(nodeId, previous, installed);
				boolean shadowReplaced = flowReplaced && SHADOW_SCRIPTS.replace(nodeId, previous, installed);
				if (!shadowReplaced || !state.activateLoaded()) {
					if (shadowReplaced) {
						SHADOW_SCRIPTS.replace(nodeId, installed, previous);
					}
					if (flowReplaced) {
						FlowBus.replaceNode(nodeId, installed, previous);
					}
					throw new ChainLoadException(StrUtil.format("script node[{}] changed or was deleted while activating", nodeId));
				}

				SCRIPT_ARTIFACTS.put(activeArtifactId,
						new ScriptArtifact(state, activeArtifactId, record.getLanguage()));
				requested.installCompiledScript(installed, record.getVersion());
				if (hadActive) {
					retired = retireScriptArtifact(state, previous);
				}
			}
			if (hadActive) {
				markReferencingChainsStale(nodeId);
			}
			unloadScriptArtifact(retired);
		}
		catch (RuntimeException e) {
			SCRIPT_ARTIFACTS.remove(activeArtifactId);
			unloadScriptArtifact(activeArtifactId, record.getLanguage());
			throw e;
		}
	}

	private static ScriptArtifact retireScriptArtifact(RuleTargetState state, Node node) {
		String artifactId = node.getRuleDbScriptArtifactId();
		if (StrUtil.isBlank(artifactId)) {
			return null;
		}
		ScriptArtifact artifact = SCRIPT_ARTIFACTS.computeIfAbsent(artifactId,
				id -> new ScriptArtifact(state, id, node.getLanguage()));
		artifact.retired = true;
		if (artifact.users == 0 && SCRIPT_ARTIFACTS.remove(artifact.id, artifact)) {
			return artifact;
		}
		return null;
	}

	private static void unloadScriptArtifact(ScriptArtifact artifact) {
		if (artifact != null) {
			unloadScriptArtifact(artifact.id, artifact.language);
		}
	}

	private static void unloadScriptArtifact(String artifactId, String language) {
		try {
			ScriptExecutorFactory.loadInstance().getScriptExecutor(language).unLoad(artifactId);
		}
		catch (RuntimeException e) {
			LOG.warn("unload script artifact[{}] failed: {}", artifactId, e.getMessage());
		}
	}

	public static ScriptExecutionLease acquireScriptExecution(Node node) {
		String nodeId = node.getId();
		while (true) {
			RuleTargetState state = SCRIPT_STATES.get(nodeId);
			if (state == null) {
				return null;
			}
			boolean reload = false;
			synchronized (state) {
				if (SCRIPT_STATES.get(nodeId) != state || state.isDeleted()) {
					throw new ChainLoadException(StrUtil.format("script node[{}] was deleted from rule repository", nodeId));
				}
				Node active = SHADOW_SCRIPTS.get(nodeId);
				if (state.getActiveVersion() == 0 || active == null || !active.isCompiled()) {
					reload = true;
				}
				else {
					if (node.getRuleDbScriptVersion() != state.getActiveVersion()) {
						node.installCompiledScript(active, state.getActiveVersion());
					}
					String artifactId = node.getRuleDbScriptArtifactId();
					if (StrUtil.isBlank(artifactId)) {
						throw new ChainLoadException(StrUtil.format("script node[{}] has no active artifact", nodeId));
					}
					ScriptArtifact artifact = SCRIPT_ARTIFACTS.computeIfAbsent(artifactId,
							id -> new ScriptArtifact(state, id, node.getLanguage()));
					if (artifact.retired) {
						reload = true;
					}
					else {
						artifact.users++;
						return new ScriptExecutionLease(artifact);
					}
				}
			}
			if (reload) {
				refreshScript(node);
			}
		}
	}

	private static void releaseScriptExecution(ScriptArtifact artifact) {
		boolean unload = false;
		synchronized (artifact.state) {
			if (artifact.users > 0) {
				artifact.users--;
			}
			if (artifact.users == 0 && artifact.retired
					&& SCRIPT_ARTIFACTS.remove(artifact.id, artifact)) {
				unload = true;
			}
		}
		if (unload) {
			unloadScriptArtifact(artifact);
		}
	}

	private static void markReferencingChainsStale(String nodeId) {
		for (Map.Entry<String, Chain> entry : FlowBus.getChainMap().entrySet()) {
			RuleTargetState chainState = CHAIN_STATES.get(entry.getKey());
			if (!isLive(chainState) || chainState.getActiveVersion() == 0) {
				continue;
			}
			try {
				for (Node referenced : LiteflowMetaOperator.getNodes(entry.getKey())) {
					if (nodeId.equals(referenced.getId())) {
						chainState.markStale();
						break;
					}
				}
			}
			catch (RuntimeException ignored) {
			}
		}
	}

	private static ChainRecord fetchChainWithRetry(String chainId) {
		int retry = retryTimes();
		RuntimeException last = null;
		for (int i = 0; i <= retry; i++) {
			try {
				return repositoryForRead().fetchChain(chainId);
			} catch (RuntimeException e) {
				last = e;
			}
		}
		throw new ChainLoadException(StrUtil.format("fetch chain[{}] failed after {} retries: {}",
				chainId, retry, last == null ? StrUtil.EMPTY : last.getMessage()));
	}

	private static ScriptRecord fetchScriptWithRetry(String nodeId) {
		int retry = retryTimes();
		RuntimeException last = null;
		for (int i = 0; i <= retry; i++) {
			try {
				return repositoryForRead().fetchScript(nodeId);
			} catch (RuntimeException e) {
				last = e;
			}
		}
		throw new ChainLoadException(StrUtil.format("fetch script[{}] failed after {} retries: {}",
				nodeId, retry, last == null ? StrUtil.EMPTY : last.getMessage()));
	}

	private static ChainMeta fetchChainMetaWithRetry(String chainId) {
		int retry = retryTimes();
		RuntimeException last = null;
		for (int i = 0; i <= retry; i++) {
			try {
				return repositoryForRead().fetchChainMeta(chainId);
			} catch (RuntimeException e) {
				last = e;
			}
		}
		throw new ChainLoadException(StrUtil.format("fetch chain metadata[{}] failed after {} retries: {}",
				chainId, retry, last == null ? StrUtil.EMPTY : last.getMessage()));
	}

	private static ScriptMeta fetchScriptMetaWithRetry(String nodeId) {
		int retry = retryTimes();
		RuntimeException last = null;
		for (int i = 0; i <= retry; i++) {
			try {
				return repositoryForRead().fetchScriptMeta(nodeId);
			} catch (RuntimeException e) {
				last = e;
			}
		}
		throw new ChainLoadException(StrUtil.format("fetch script metadata[{}] failed after {} retries: {}",
				nodeId, retry, last == null ? StrUtil.EMPTY : last.getMessage()));
	}

	private static int retryTimes() {
		RuleDbConfig ruleDb = LiteflowConfigGetter.get().getRuleDb();
		return ruleDb == null || ruleDb.getSync() == null || ruleDb.getSync().getFetchRetryTimes() == null
				? 3 : ruleDb.getSync().getFetchRetryTimes();
	}

	private static RuleRepository repositoryForRead() {
		RuleRepository repository = activeRepository;
		if (repository != null) {
			return repository;
		}
		RuleDbProvider provider = RuleDbProviderHolder.get();
		if (provider != null && provider.repository() != null) {
			return provider.repository();
		}
		return null;
	}

	/** 编译完成后登记：chain 驻留缓存 + 收集引用的脚本节点（引用计数+1） */
	public static void recordCompiledChain(String chainId) {
		recordCompiledChain(chainId, FlowBus.getChain(chainId));
	}

	public static void recordCompiledChain(Chain chain) {
		if (chain != null) {
			recordCompiledChain(chain.getChainId(), chain);
		}
	}

	private static synchronized void recordCompiledChain(String chainId, Chain compiledChain) {
		RuleTargetState state = CHAIN_STATES.get(chainId);
		RuleLoadAttempt loadAttempt = compiledChain == null ? null : CHAIN_LOAD_STATES.remove(compiledChain);
		if (loadAttempt != null && loadAttempt.state != state) {
			if (compiledChain != null) {
				compiledChain.setCompiled(false);
				restoreOrRemoveLateChain(chainId, compiledChain);
			}
			return;
		}
		if (state != null && !state.isDeleted() && compiledChain != SHADOW_CHAINS.get(chainId)) {
			if (compiledChain != null) {
				compiledChain.setCompiled(false);
				restoreOrRemoveLateChain(chainId, compiledChain);
			}
			return;
		}
		if (state != null && !state.activateLoaded()) {
			if (state.isDeleted()) {
				if (compiledChain == null) {
					removeOwnedChain(chainId);
				} else {
					SHADOW_CHAINS.remove(chainId, compiledChain);
					restoreOrRemoveLateChain(chainId, compiledChain);
				}
			} else if (compiledChain != null) {
				compiledChain.setCompiled(false);
				restoreOrRemoveLateChain(chainId, compiledChain);
			}
			return;
		}
		recordChainCache(chainId);
	}

	private static void recordChainCache(String chainId) {
		List<String> scriptRefs = new ArrayList<>();
		try {
			for (Node n : LiteflowMetaOperator.getNodes(chainId)) {
				if (n.getType() != null && n.getType().isScript() && isLive(SCRIPT_STATES.get(n.getId()))) {
					scriptRefs.add(n.getId());
				}
			}
		} catch (Exception ignored) {
		}
		RuleDbCache.recordChainAccess(chainId, scriptRefs);
	}

	public static void recordCompiledScript(String nodeId) {
		recordCompiledScript(nodeId, FlowBus.getNode(nodeId));
	}

	public static void recordCompiledScript(Node node) {
		if (node != null) {
			recordCompiledScript(node.getId(), node);
		}
	}

	private static synchronized void recordCompiledScript(String nodeId, Node compiledNode) {
		RuleTargetState state = SCRIPT_STATES.get(nodeId);
		RuleLoadAttempt loadAttempt = compiledNode == null ? null : SCRIPT_LOAD_STATES.get(compiledNode);
		if (state != null && (loadAttempt == null || loadAttempt.state != state || state.isDeleted())) {
			if (compiledNode != null) {
				SCRIPT_LOAD_STATES.remove(compiledNode, loadAttempt);
				compiledNode.setCompiled(false);
				restoreOrRemoveLateScript(nodeId, compiledNode);
			}
			return;
		}
		if (state == null || compiledNode == null) {
			return;
		}
		Node owned = SHADOW_SCRIPTS.get(nodeId);
		if (!state.isDesiredLoaded() || owned == null || FlowBus.getNode(nodeId) != owned
				|| (owned != compiledNode && !FlowBus.replaceNode(nodeId, owned, compiledNode))) {
			SCRIPT_LOAD_STATES.remove(compiledNode, loadAttempt);
			compiledNode.setCompiled(false);
			restoreOrRemoveLateScript(nodeId, compiledNode);
			return;
		}
		if (owned != compiledNode && !SHADOW_SCRIPTS.replace(nodeId, owned, compiledNode)) {
			FlowBus.replaceNode(nodeId, compiledNode, owned);
			SCRIPT_LOAD_STATES.remove(compiledNode, loadAttempt);
			compiledNode.setCompiled(false);
			return;
		}
		if (!state.activateLoaded()) {
			SHADOW_SCRIPTS.replace(nodeId, compiledNode, owned);
			FlowBus.replaceNode(nodeId, compiledNode, owned);
			SCRIPT_LOAD_STATES.remove(compiledNode, loadAttempt);
			compiledNode.setCompiled(false);
			return;
		}
		SCRIPT_LOAD_STATES.remove(compiledNode, loadAttempt);
	}

	public static void markChainLoadFailed(String chainId, Throwable error) {
		markChainLoadFailed(FlowBus.getChain(chainId), error);
	}

	public static void markChainLoadFailed(Chain chain, Throwable error) {
		if (chain == null) {
			return;
		}
		RuleLoadAttempt attempt = CHAIN_LOAD_STATES.remove(chain);
		if (attempt != null) {
			attempt.markFailedIfCurrent(CHAIN_STATES.get(chain.getChainId()), error);
		}
	}

	public static void markScriptLoadFailed(String nodeId, Throwable error) {
		markScriptLoadFailed(FlowBus.getNode(nodeId), error);
	}

	public static void markScriptLoadFailed(Node node, Throwable error) {
		if (node != null) {
			RuleLoadAttempt attempt = SCRIPT_LOAD_STATES.remove(node);
			if (attempt != null) {
				attempt.markFailedIfCurrent(SCRIPT_STATES.get(node.getId()), error);
			}
		}
	}

	public static boolean isChainStale(String chainId) {
		RuleTargetState state = CHAIN_STATES.get(chainId);
		return isLive(state) && state.getStatus() == RuleTargetStatus.STALE;
	}

	public static boolean isManagedChain(String chainId) {
		return isLive(CHAIN_STATES.get(chainId));
	}

	public static boolean isManagedScript(String nodeId) {
		return SCRIPT_STATES.get(nodeId) != null;
	}

	public static boolean isScriptStale(String nodeId) {
		RuleTargetState state = SCRIPT_STATES.get(nodeId);
		return state != null && (state.getStatus() == RuleTargetStatus.STALE || state.isDeleted());
	}

	// ---- 索引/缓存态操作，供 Task 4/5 的 Cache/SyncManager 使用 ----

	public static Long getChainVersion(String chainId) {
		RuleTargetState state = CHAIN_STATES.get(chainId);
		return isLive(state) ? state.getDesiredVersion() : null;
	}

	public static Map<String, Long> scriptVersionIndex() {
		Map<String, Long> versions = new ConcurrentHashMap<>();
		for (Map.Entry<String, RuleTargetState> entry : SCRIPT_STATES.entrySet()) {
			if (isLive(entry.getValue())) {
				versions.put(entry.getKey(), entry.getValue().getDesiredVersion());
			}
		}
		return versions;
	}

	public static RuleTargetState chainState(String chainId) {
		return CHAIN_STATES.get(chainId);
	}

	public static RuleTargetState scriptState(String nodeId) {
		return SCRIPT_STATES.get(nodeId);
	}

	/** Returns a bounded diagnostic snapshot without rule or script content. */
	public static RuleDbRuntimeSnapshot snapshot() {
		if (!initialized || !RuleDbSyncManager.isOpen()) {
			return RuleDbRuntimeSnapshot.inactive();
		}
		EnumMap<RuleTargetStatus, Integer> counts = new EnumMap<>(RuleTargetStatus.class);
		for (RuleTargetStatus status : RuleTargetStatus.values()) {
			counts.put(status, 0);
		}
		List<RuleDbRuntimeSnapshot.FailedTarget> failures = new ArrayList<>();
		collectSnapshotTargets(CHAIN_STATES, ChangeRecord.TargetType.CHAIN, counts, failures);
		collectSnapshotTargets(SCRIPT_STATES, ChangeRecord.TargetType.SCRIPT, counts, failures);
		return new RuleDbRuntimeSnapshot(true, RuleDbSyncManager.providerType(),
				RuleDbSyncManager.changeSourceHealth(), LAST_APPLIED_SEQ.get(),
				RuleDbSyncManager.lastSuccessfulReconcileTime(), RuleDbSyncManager.lastReconcileError(),
				counts, failures);
	}

	private static void collectSnapshotTargets(Map<String, RuleTargetState> states,
			ChangeRecord.TargetType targetType, Map<RuleTargetStatus, Integer> counts,
			List<RuleDbRuntimeSnapshot.FailedTarget> failures) {
		List<String> ids = new ArrayList<>(states.keySet());
		Collections.sort(ids);
		for (String id : ids) {
			RuleTargetState state = states.get(id);
			if (state == null) {
				continue;
			}
			synchronized (state) {
				RuleTargetStatus status = state.getStatus();
				counts.put(status, counts.get(status) + 1);
				if (status == RuleTargetStatus.FAILED && failures.size() < MAX_FAILED_TARGETS) {
					failures.add(new RuleDbRuntimeSnapshot.FailedTarget(targetType, id, status,
							state.getDesiredVersion(), state.getActiveVersion(),
							errorSummary(state.getLastError())));
				}
			}
		}
	}

	private static String errorSummary(Throwable error) {
		if (error == null) {
			return null;
		}
		String message = StrUtil.isBlank(error.getMessage())
				? error.getClass().getSimpleName() : error.getMessage();
		return message.length() <= 256 ? message : message.substring(0, 256);
	}

	static void onChainEvicted(String chainId) {
		RuleTargetState state = CHAIN_STATES.get(chainId);
		if (state != null) {
			state.clearActive();
		}
	}

	static void onScriptEvicted(String nodeId) {
		RuleTargetState state = SCRIPT_STATES.get(nodeId);
		ScriptArtifact retired = null;
		if (state != null) {
			synchronized (state) {
				Node active = SHADOW_SCRIPTS.get(nodeId);
				if (SCRIPT_STATES.get(nodeId) == state && active != null) {
					retired = retireScriptArtifact(state, active);
					active.clearCompiledScript();
					state.clearActive();
				}
			}
		}
		unloadScriptArtifact(retired);
	}

	/** Marks a loaded chain for refresh without discarding its active fields. */
	static void invalidateChainCache(String chainId) {
		Chain chain = FlowBus.getChain(chainId);
		if (chain != null) {
			chain.setCompiled(false);
		}
	}

	static void invalidateScriptCache(String nodeId) {
		Node node = FlowBus.getNode(nodeId);
		if (node != null) {
			node.setScript(null);
			node.setCompiled(false);
		}
		invalidateScriptClones(nodeId);
	}

	private static void invalidateScriptClones(String nodeId) {
		// 已编译 chain 的条件树里持有的是克隆 Node（浅拷贝连 isCompiled/instance 一起拷），
		// 只失效 nodeMap 驻留的那一个时，其余克隆仍是已编译态，在别的 chain 重载执行器产物之前
		// 会一直跑旧脚本——必须把所有条件树里的同 id 克隆一并失效（对齐 FlowBus.reloadScript 的克隆更新语义）
		Set<Executable> visited = Collections.newSetFromMap(new IdentityHashMap<Executable, Boolean>());
		for (Chain chain : FlowBus.getChainMap().values()) {
			invalidateScriptClones(nodeId, chain, visited);
		}
	}

	private static void invalidateScriptClones(String nodeId, Executable executable, Set<Executable> visited) {
		if (executable == null || !visited.add(executable)) {
			return;
		}
		if (executable instanceof Node) {
			Node node = (Node) executable;
			if (nodeId.equals(node.getId())) {
				node.setScript(null);
				node.setCompiled(false);
			}
			return;
		}
		if (executable instanceof Chain) {
			List<Condition> conditions = ((Chain) executable).getConditionList();
			if (CollUtil.isEmpty(conditions)) {
				return;
			}
			for (Condition condition : conditions) {
				invalidateScriptClones(nodeId, condition, visited);
			}
			return;
		}
		if (executable instanceof Condition) {
			for (List<Executable> group : ((Condition) executable).getExecutableGroup().values()) {
				if (CollUtil.isEmpty(group)) {
					continue;
				}
				for (Executable item : group) {
					invalidateScriptClones(nodeId, item, visited);
				}
			}
		}
	}

	private static boolean scriptGenerationsCurrent(Executable executable, Set<Executable> visited) {
		if (executable == null || !visited.add(executable)) {
			return true;
		}
		if (executable instanceof Node) {
			Node node = (Node) executable;
			RuleTargetState scriptState = SCRIPT_STATES.get(node.getId());
			return !isLive(scriptState) || scriptState.getActiveVersion() == 0
					|| node.getRuleDbScriptVersion() == scriptState.getActiveVersion();
		}
		if (executable instanceof Chain) {
			List<Condition> conditions = ((Chain) executable).getConditionList();
			if (CollUtil.isEmpty(conditions)) {
				return true;
			}
			for (Condition condition : conditions) {
				if (!scriptGenerationsCurrent(condition, visited)) {
					return false;
				}
			}
			return true;
		}
		if (executable instanceof Condition) {
			for (List<Executable> group : ((Condition) executable).getExecutableGroup().values()) {
				if (CollUtil.isEmpty(group)) {
					continue;
				}
				for (Executable item : group) {
					if (!scriptGenerationsCurrent(item, visited)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * 单条变更处理（分级刷新 / 惰性失效）：
	 * - UPSERT chain：更新索引版本；新增则注册影子；复用 {@link #invalidateChainCache} 失效缓存态，下次执行懒加载新版。
	 * - UPSERT script：更新索引；复用 {@link #invalidateScriptCache} 清缓存态让下次 getInstance 回源重编。
	 * - DELETE：移除索引 + 缓存态 + FlowBus 中的条目。
	 * 由 {@link RuleDbSyncManager} 的轮询/订阅路径回调。
	 */
	public static synchronized void applyChange(ChangeRecord change) {
		String id = change.getTargetId();
		long version = change.getVersion();
		if (change.getTargetType() == ChangeRecord.TargetType.CHAIN) {
			assertNoForeignChain(id);
			if (change.getOp() == ChangeRecord.Op.DELETE) {
				CHAIN_STATES.computeIfAbsent(id, key -> new RuleTargetState()).markDeleted();
				removeOwnedChain(id);
			} else {
				RuleTargetState state = CHAIN_STATES.get(id);
				if (isLive(state) && version <= state.getDesiredVersion()) {
					return;
				}
				if (!isLive(state)) {
					ChainMeta meta = fetchChainMetaWithRetry(id);
					registerShadowChain(meta == null ? new ChainMeta(id, version, null) : meta);
					state = CHAIN_STATES.get(id);
					if (state != null && version > state.getDesiredVersion()) {
						state.updateDesired(version, null);
					}
					return;
				}
				state.updateDesired(version, null);
			}
		} else {
			assertNoForeignScript(id);
			if (change.getOp() == ChangeRecord.Op.DELETE) {
				SCRIPT_STATES.computeIfAbsent(id, key -> new RuleTargetState()).markDeleted();
				removeOwnedScript(id);
			} else {
				RuleTargetState state = SCRIPT_STATES.get(id);
				if (isLive(state) && version <= state.getDesiredVersion()) {
					return;
				}
				if (!isLive(state)) {
					// 新增脚本：轮询/订阅的 ChangeRecord 不带 type/language 元数据，
					// 回源取一次注册影子 Node，否则引用它的 chain 在下次全量对账前都编译不过
					ScriptMeta meta = fetchScriptMetaWithRetry(id);
					if (meta == null) {
						return; // 已被删除/停用：等后续 DELETE 变更或对账处理
					}
					registerShadowScript(meta);
					if (!SHADOW_SCRIPTS.containsKey(id)) {
						return;
					}
					state = SCRIPT_STATES.get(id);
					if (state != null && version > state.getDesiredVersion()) {
						state.updateDesired(version, null);
					}
					return;
				}
				state.updateDesired(version, null);
			}
		}
	}

	/**
	 * 全量对账：以 manifest 为准修正索引与缓存。
	 * version 不一致更新 desired 状态；version 相同再比 content_md5（双保险，spec §7），
	 * 发现"改了内容没动版本号"的脏写同样按 UPSERT 失效；清单中已消失的条目触发 DELETE；
	 * 新增 script（索引中无）登记影子。
	 * 由 {@link RuleDbSyncManager#reconcileOnce()} 回调。
	 */
	public static synchronized void reconcile(RuleManifest manifest) {
		validateManifest(manifest);
		validateManifestOwnership(manifest);
		Set<String> liveChains = new HashSet<>();
		if (manifest.getChains() != null) {
			for (ChainMeta cm : manifest.getChains()) {
				liveChains.add(cm.getChainId());
				RuleTargetState state = CHAIN_STATES.get(cm.getChainId());
				if (!isLive(state)) {
					registerShadowChain(cm);
				} else if (cm.getVersion() > state.getDesiredVersion()
						|| (cm.getVersion() == state.getDesiredVersion()
						&& md5Mismatch(state.getDesiredMd5(), cm.getMd5()))) {
					state.updateDesired(cm.getVersion(), cm.getMd5());
				}
			}
		}
		// 清单中已消失的 chain（且属 rule-db 管理）删除
		for (String chainId : new ArrayList<>(CHAIN_STATES.keySet())) {
			if (isLive(CHAIN_STATES.get(chainId)) && !liveChains.contains(chainId)) {
				applyChange(new ChangeRecord(0, ChangeRecord.TargetType.CHAIN,
						chainId, ChangeRecord.Op.DELETE, 0));
			}
		}
		Set<String> liveScripts = new HashSet<>();
		if (manifest.getScripts() != null) {
			for (ScriptMeta sm : manifest.getScripts()) {
				liveScripts.add(sm.getNodeId());
				RuleTargetState state = SCRIPT_STATES.get(sm.getNodeId());
				if (!isLive(state)) {
					// 新增脚本：登记影子 Node + 版本戳
					registerShadowScript(sm);
				} else if (sm.getVersion() > state.getDesiredVersion()
						|| (sm.getVersion() == state.getDesiredVersion()
						&& md5Mismatch(state.getDesiredMd5(), sm.getMd5()))) {
					state.updateDesired(sm.getVersion(), sm.getMd5());
				}
			}
		}
		for (String nodeId : new ArrayList<>(SCRIPT_STATES.keySet())) {
			if (isLive(SCRIPT_STATES.get(nodeId)) && !liveScripts.contains(nodeId)) {
				applyChange(new ChangeRecord(0, ChangeRecord.TargetType.SCRIPT,
						nodeId, ChangeRecord.Op.DELETE, 0));
			}
		}
	}

	/** 双方 md5 都在手才比较；缓存态没有 md5（影子/未回源）时不构成脏写信号 */
	private static boolean md5Mismatch(String cachedMd5, String manifestMd5) {
		return StrUtil.isNotBlank(cachedMd5) && StrUtil.isNotBlank(manifestMd5) && !cachedMd5.equals(manifestMd5);
	}

	static void validateManifest(RuleManifest manifest) {
		if (manifest == null) {
			throw new ConfigErrorException("rule-db manifest must not be null");
		}
		if (manifest.getLatestSeq() < 0) {
			throw new ConfigErrorException("rule-db manifest latestSeq must not be negative");
		}
		Set<String> chainIds = new HashSet<>();
		if (manifest.getChains() != null) {
			for (ChainMeta meta : manifest.getChains()) {
				if (meta == null || StrUtil.isBlank(meta.getChainId()) || meta.getVersion() <= 0) {
					throw new ConfigErrorException("rule-db manifest contains invalid chain metadata");
				}
				if (!chainIds.add(meta.getChainId())) {
					throw new ConfigErrorException("rule-db manifest contains duplicate chain[" + meta.getChainId() + "]");
				}
			}
		}
		Set<String> scriptIds = new HashSet<>();
		if (manifest.getScripts() != null) {
			for (ScriptMeta meta : manifest.getScripts()) {
				NodeTypeEnum type = meta == null ? null : NodeTypeEnum.getEnumByCode(meta.getType());
				if (meta == null || StrUtil.isBlank(meta.getNodeId()) || meta.getVersion() <= 0
						|| type == null || !type.isScript()) {
					throw new ConfigErrorException("rule-db manifest contains invalid script metadata");
				}
				if (!scriptIds.add(meta.getNodeId())) {
					throw new ConfigErrorException("rule-db manifest contains duplicate script[" + meta.getNodeId() + "]");
				}
			}
		}
	}

	private static void validateManifestOwnership(RuleManifest manifest) {
		if (manifest.getChains() != null) {
			for (ChainMeta meta : manifest.getChains()) {
				assertNoForeignChain(meta.getChainId());
			}
		}
		if (manifest.getScripts() != null) {
			for (ScriptMeta meta : manifest.getScripts()) {
				assertNoForeignScript(meta.getNodeId());
			}
		}
	}

	private static void assertNoForeignChain(String chainId) {
		Chain current = FlowBus.getChain(chainId);
		if (current != null && current != SHADOW_CHAINS.get(chainId)) {
			throw collision("chain", chainId);
		}
	}

	private static void assertNoForeignScript(String nodeId) {
		Node current = FlowBus.getNode(nodeId);
		if (current != null && current != SHADOW_SCRIPTS.get(nodeId)) {
			throw collision("script", nodeId);
		}
	}

	private static ConfigErrorException collision(String type, String id) {
		return new ConfigErrorException("rule-db " + type + "[" + id
				+ "] collides with application-owned FlowBus metadata");
	}

	private static void restoreOrRemoveLateChain(String chainId, Chain lateInstalled) {
		Chain owned = SHADOW_CHAINS.get(chainId);
		if (owned == lateInstalled && isLive(CHAIN_STATES.get(chainId))) {
			return;
		}
		if (owned != null && owned != lateInstalled && isLive(CHAIN_STATES.get(chainId))) {
			FlowBus.replaceChain(chainId, lateInstalled, owned);
		} else {
			FlowBus.removeChain(chainId, lateInstalled);
		}
	}

	private static void restoreOrRemoveLateScript(String nodeId, Node lateInstalled) {
		Node owned = SHADOW_SCRIPTS.get(nodeId);
		if (owned != null && owned != lateInstalled && isLive(SCRIPT_STATES.get(nodeId))) {
			FlowBus.replaceNode(nodeId, lateInstalled, owned);
		} else {
			FlowBus.removeNode(nodeId, lateInstalled);
		}
	}

	private static void removeOwnedChain(String chainId) {
		Chain owned = SHADOW_CHAINS.remove(chainId);
		if (owned != null) {
			FlowBus.removeChain(chainId, owned);
		}
	}

	private static void removeOwnedScript(String nodeId) {
		invalidateScriptClones(nodeId);
		Node owned = SHADOW_SCRIPTS.remove(nodeId);
		ScriptArtifact retired = null;
		if (owned != null) {
			RuleTargetState state = SCRIPT_STATES.get(nodeId);
			if (state != null) {
				synchronized (state) {
					retired = retireScriptArtifact(state, owned);
					owned.clearCompiledScript();
				}
			}
			FlowBus.removeNode(nodeId, owned);
		}
		unloadScriptArtifact(retired);
	}

	public static synchronized void destroy() {
		RuleDbSyncManager.stop();
		clearRuntimeState();
		// 重置 isActive 缓存，使下次 init 重新计算（非 rule-db 应用 destroy 后也不残留过期 true）
		activeFlag = null;
	}

	/** Clears all runtime-owned state after a failed activation or explicit destroy. */
	private static void clearRuntimeState() {
		RuleDbCache.destroy();
		for (ScriptArtifact artifact : new ArrayList<>(SCRIPT_ARTIFACTS.values())) {
			if (SCRIPT_ARTIFACTS.remove(artifact.id, artifact)) {
				unloadScriptArtifact(artifact);
			}
		}
		for (Map.Entry<String, Chain> entry : SHADOW_CHAINS.entrySet()) {
			if (FlowBus.getChain(entry.getKey()) == entry.getValue()) {
				FlowBus.removeChain(entry.getKey());
			}
		}
		for (Map.Entry<String, Node> entry : SHADOW_SCRIPTS.entrySet()) {
			if (FlowBus.getNode(entry.getKey()) == entry.getValue()) {
				FlowBus.removeNode(entry.getKey());
			}
		}
		SHADOW_CHAINS.clear();
		SHADOW_SCRIPTS.clear();
		CHAIN_LOAD_STATES.clear();
		SCRIPT_LOAD_STATES.clear();
		SCRIPT_ARTIFACTS.clear();
		CHAIN_STATES.clear();
		SCRIPT_STATES.clear();
		LAST_APPLIED_SEQ.set(0);
		activeRepository = null;
		initialized = false;
	}
}
