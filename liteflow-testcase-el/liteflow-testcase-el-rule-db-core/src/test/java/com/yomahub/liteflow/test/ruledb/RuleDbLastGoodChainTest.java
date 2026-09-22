package com.yomahub.liteflow.test.ruledb;

import cn.hutool.crypto.digest.MD5;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import com.yomahub.liteflow.exception.NoMatchedRouteChainException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.RuleDbConfig;
import com.yomahub.liteflow.repository.RuleDbRuntime;
import com.yomahub.liteflow.repository.RuleDbSyncManager;
import com.yomahub.liteflow.repository.runtime.RuleTargetState;
import com.yomahub.liteflow.repository.runtime.RuleTargetStatus;
import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RuleDbLastGoodChainTest extends BaseRuleDbTest {

	@Test
	public void testColdRouteQueryLoadsRuleDbMetadataOnDemand() {
		putRouteChain("route1", "THEN(a, b)", "route", "orders");
		registerCommonCmp();
		registerRouteCmp();
		FlowExecutor executor = buildExecutor(new RuleDbConfig());

		List<LiteflowResponse> responses = executor.executeRouteChain("orders", true, DefaultContext.class);

		Assertions.assertEquals(1, responses.size());
		Assertions.assertEquals("route1", responses.get(0).getChainId());
		Assertions.assertEquals("a==>b", responses.get(0).getExecuteStepStr());
		Assertions.assertEquals(1L, RuleDbRuntime.chainState("route1").getActiveVersion());
	}

	@Test
	public void testRouteQueryRefreshesVersionTwoRoute() {
		putRouteChain("route1", "THEN(a)", "route", "orders");
		registerCommonCmp();
		registerRouteCmp();
		FlowExecutor executor = buildExecutor(new RuleDbConfig());
		Assertions.assertEquals(1,
				executor.executeRouteChain("orders", true, DefaultContext.class).size());

		publishRouteChain("route1", "THEN(a)", "NOT(route)", "orders");
		RuleDbSyncManager.pollOnce();

		Assertions.assertThrows(NoMatchedRouteChainException.class,
				() -> executor.executeRouteChain("orders", true, DefaultContext.class));
		Assertions.assertEquals(2L, RuleDbRuntime.chainState("route1").getActiveVersion());
	}

	@Test
	public void testExecuteWithElReusesLoadedRuleDbChain() {
		FlowExecutor executor = loadVersionOne("THEN(a, b)");

		LiteflowResponse response = executor.execute2RespWithEL("THEN(a, b)");

		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("chain1", response.getChainId());
	}

	@Test
	public void testEnsureChainLoadedUsesOriginalElFingerprint() {
		String el = "THEN(a.tag(\"hello world\"));";
		InMemoryRuleRepository.putChain("chain1", el);
		registerCommonCmp();
		buildExecutor(new RuleDbConfig());

		RuleDbRuntime.ensureChainLoaded("chain1");

		Assertions.assertEquals(el, FlowBus.getChain("chain1").getEl());
		Assertions.assertEquals(elMd5(el), FlowBus.getChain("chain1").getElMd5());
	}

	@Test
	public void testLiteralWhitespaceChangeReplacesElMapping() {
		String versionOneEl = "THEN(a.tag(\"a b\"));";
		String versionTwoEl = "THEN(a.tag(\"ab\"));";
		FlowExecutor executor = loadVersionOne(versionOneEl);
		Assertions.assertEquals("chain1", FlowBus.getChainIdByElMd5(elMd5(versionOneEl)));

		InMemoryRuleRepository.publishChain("chain1", versionTwoEl);
		RuleDbSyncManager.pollOnce();
		LiteflowResponse updated = executor.execute2Resp("chain1");
		Assertions.assertTrue(updated.isSuccess(), updated.getMessage());
		Assertions.assertEquals("ab", updated.getExecuteStepQueue().peek().getTag());
		Assertions.assertNull(FlowBus.getChainIdByElMd5(elMd5(versionOneEl)));
		Assertions.assertEquals("chain1", FlowBus.getChainIdByElMd5(elMd5(versionTwoEl)));
		Assertions.assertEquals("chain1", executor.execute2RespWithEL(versionTwoEl).getChainId());

		LiteflowResponse original = executor.execute2RespWithEL(versionOneEl);
		Assertions.assertTrue(original.isSuccess(), original.getMessage());
		Assertions.assertNotEquals("chain1", original.getChainId());
		Assertions.assertEquals("a b", original.getExecuteStepQueue().peek().getTag());
	}

	@Test
	public void testRuleDbElMappingTakesOverEarlierTransientChain() {
		String el = "THEN(a, b)";
		InMemoryRuleRepository.putChain("chain1", el);
		registerCommonCmp();
		FlowExecutor executor = buildExecutor(new RuleDbConfig());

		String transientId = executor.execute2RespWithEL(el).getChainId();
		Assertions.assertNotEquals("chain1", transientId);
		Assertions.assertTrue(FlowBus.getChain(transientId).isTransientElChain());

		Assertions.assertTrue(executor.execute2Resp("chain1", "arg").isSuccess());
		Assertions.assertEquals("chain1", FlowBus.getChainIdByElMd5(elMd5(el)));
		Assertions.assertTrue(FlowBus.removeChain(transientId));
		Assertions.assertEquals("chain1", FlowBus.getChainIdByElMd5(elMd5(el)));
	}

	@Test
	public void testLateTransientChainCannotOverwriteRuleDbElMapping() {
		String el = "THEN(a, b)";
		FlowExecutor executor = loadVersionOne(el);

		LiteFlowChainELBuilder.createChain()
				.setChainId("lateTransient")
				.setTransientElChain(true)
				.setEL(el)
				.build();

		Assertions.assertEquals("chain1", FlowBus.getChainIdByElMd5(elMd5(el)));
		Assertions.assertEquals("chain1", executor.execute2RespWithEL(el).getChainId());
	}

	@Test
	public void testRuleDbElMappingDoesNotOverwriteAnotherChain() {
		String el = "THEN(a, b)";
		InMemoryRuleRepository.putChain("chain1", el);
		registerCommonCmp();
		FlowExecutor executor = buildExecutor(new RuleDbConfig());
		LiteFlowChainELBuilder.createChain().setChainId("application").setEL(el).build();

		Assertions.assertTrue(executor.execute2Resp("chain1", "arg").isSuccess());

		Assertions.assertEquals("application", FlowBus.getChainIdByElMd5(elMd5(el)));
		RuleDbRuntime.destroy();
		Assertions.assertEquals("application", FlowBus.getChainIdByElMd5(elMd5(el)));
	}

	@Test
	public void testVersionTwoReplacesRuleDbElMd5Mapping() {
		String versionOneEl = "THEN(a, b)";
		String versionTwoEl = "THEN(b, a)";
		FlowExecutor executor = loadVersionOne(versionOneEl);
		String versionOneMd5 = elMd5(versionOneEl);
		Assertions.assertEquals("chain1", FlowBus.getChainIdByElMd5(versionOneMd5));

		InMemoryRuleRepository.publishChain("chain1", versionTwoEl);
		RuleDbSyncManager.pollOnce();
		Assertions.assertTrue(executor.execute2Resp("chain1", "arg").isSuccess());

		String versionTwoMd5 = elMd5(versionTwoEl);
		Assertions.assertNull(FlowBus.getChainIdByElMd5(versionOneMd5));
		Assertions.assertEquals("chain1", FlowBus.getChainIdByElMd5(versionTwoMd5));
		Assertions.assertEquals("chain1", executor.execute2RespWithEL(versionTwoEl).getChainId());
		Assertions.assertNotEquals("chain1", executor.execute2RespWithEL(versionOneEl).getChainId());
	}

	@Test
	public void testInvalidNewElKeepsVersionOneExecutable() {
		FlowExecutor executor = loadVersionOne("THEN(a, b)");

		InMemoryRuleRepository.publishChain("chain1", "THEN(a, missing)");
		RuleDbSyncManager.pollOnce();

		LiteflowResponse response = executor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b", response.getExecuteStepStr());
		RuleTargetState state = RuleDbRuntime.chainState("chain1");
		Assertions.assertEquals(2L, state.getDesiredVersion());
		Assertions.assertEquals(1L, state.getActiveVersion());
		Assertions.assertEquals(RuleTargetStatus.FAILED, state.getStatus());
	}

	@Test
	public void testFirstInvalidVersionFailsWithoutActiveGeneration() {
		InMemoryRuleRepository.putChain("chain1", "THEN(a, missing)");
		registerCommonCmp();
		FlowExecutor executor = buildExecutor(new RuleDbConfig());

		LiteflowResponse response = executor.execute2Resp("chain1", "arg");

		Assertions.assertFalse(response.isSuccess());
		RuleTargetState state = RuleDbRuntime.chainState("chain1");
		Assertions.assertEquals(0L, state.getActiveVersion());
		Assertions.assertEquals(RuleTargetStatus.FAILED, state.getStatus());
		Assertions.assertNotNull(state.getLastError());
	}

	@Test
	public void testSuccessfulVersionTwoAtomicallyReplacesVersionOne() {
		FlowExecutor executor = loadVersionOne("THEN(a, b)");

		InMemoryRuleRepository.publishChain("chain1", "THEN(b, a)");
		RuleDbSyncManager.pollOnce();

		LiteflowResponse response = executor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("b==>a", response.getExecuteStepStr());
		RuleTargetState state = RuleDbRuntime.chainState("chain1");
		Assertions.assertEquals(2L, state.getDesiredVersion());
		Assertions.assertEquals(2L, state.getActiveVersion());
		Assertions.assertEquals(RuleTargetStatus.READY, state.getStatus());
		assertNoCandidateChainPublished();
	}

	@Test
	public void testConcurrentRefreshFetchesOnceWhileFollowersUseVersionOne() throws Exception {
		FlowExecutor executor = loadVersionOne("THEN(a, b)");
		InMemoryRuleRepository.publishChain("chain1", "THEN(b, a)");
		RuleDbSyncManager.pollOnce();
		CountDownLatch candidateFetched = new CountDownLatch(1);
		CountDownLatch releaseCandidate = new CountDownLatch(1);
		InMemoryRuleRepository.afterNextChainFetch(() -> {
			candidateFetched.countDown();
			await(releaseCandidate);
		});

		ExecutorService pool = Executors.newFixedThreadPool(8);
		try {
			Future<LiteflowResponse> leader = pool.submit(() -> executor.execute2Resp("chain1", "arg"));
			Assertions.assertTrue(candidateFetched.await(5, TimeUnit.SECONDS));
			List<Future<LiteflowResponse>> followers = new ArrayList<>();
			for (int i = 0; i < 6; i++) {
				followers.add(pool.submit(() -> executor.execute2Resp("chain1", "arg")));
			}
			for (Future<LiteflowResponse> follower : followers) {
				LiteflowResponse response = follower.get(2, TimeUnit.SECONDS);
				Assertions.assertTrue(response.isSuccess());
				Assertions.assertEquals("a==>b", response.getExecuteStepStr());
			}
			Assertions.assertEquals(2, InMemoryRuleRepository.FETCH_CHAIN_COUNT.get());
			releaseCandidate.countDown();
			LiteflowResponse leaderResponse = leader.get(5, TimeUnit.SECONDS);
			Assertions.assertTrue(leaderResponse.isSuccess());
			Assertions.assertEquals("b==>a", leaderResponse.getExecuteStepStr());
			Assertions.assertEquals(2L, RuleDbRuntime.chainState("chain1").getActiveVersion());
		} finally {
			releaseCandidate.countDown();
			pool.shutdownNow();
		}
	}

	@Test
	public void testColdExecuteDoesNotDeadlockWithRoutePreparation() throws Exception {
		putRouteChain("route1", "THEN(a, b)", "route", "orders");
		registerCommonCmp();
		registerRouteCmp();
		FlowExecutor executor = buildExecutor(new RuleDbConfig());
		CountDownLatch candidateFetched = new CountDownLatch(1);
		CountDownLatch releaseCandidate = new CountDownLatch(1);
		InMemoryRuleRepository.afterNextChainFetch(() -> {
			candidateFetched.countDown();
			await(releaseCandidate);
		});

		ExecutorService pool = Executors.newFixedThreadPool(2, runnable -> {
			Thread thread = new Thread(runnable, "rule-db-deadlock-test");
			thread.setDaemon(true);
			return thread;
		});
		AtomicReference<Thread> executeThread = new AtomicReference<>();
		try {
			Future<?> routePreparation = pool.submit(RuleDbRuntime::prepareRouteChains);
			Assertions.assertTrue(candidateFetched.await(5, TimeUnit.SECONDS));
			Future<LiteflowResponse> execution = pool.submit(() -> {
				executeThread.set(Thread.currentThread());
				return executor.execute2Resp("route1", "arg");
			});
			waitUntil(() -> {
				Thread thread = executeThread.get();
				return thread != null && thread.getState() == Thread.State.WAITING;
			}, 2000);

			releaseCandidate.countDown();
			routePreparation.get(2, TimeUnit.SECONDS);
			LiteflowResponse response = execution.get(2, TimeUnit.SECONDS);
			Assertions.assertTrue(response.isSuccess());
			Assertions.assertEquals("a==>b", response.getExecuteStepStr());
		} finally {
			releaseCandidate.countDown();
			pool.shutdownNow();
		}
	}

	@Test
	public void testMetadataChangeBetweenContentReadsRejectsCandidate() {
		FlowExecutor executor = loadVersionOne("THEN(a, b)");
		InMemoryRuleRepository.publishChain("chain1", "THEN(b, a)");
		RuleDbSyncManager.pollOnce();
		InMemoryRuleRepository.afterNextChainFetch(
				() -> InMemoryRuleRepository.putChain("chain1", "THEN(c, a)"));

		LiteflowResponse response = executor.execute2Resp("chain1", "arg");

		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b", response.getExecuteStepStr());
		RuleTargetState state = RuleDbRuntime.chainState("chain1");
		Assertions.assertEquals(1L, state.getActiveVersion());
		Assertions.assertEquals(RuleTargetStatus.FAILED, state.getStatus());
		assertNoCandidateChainPublished();
	}

	@Test
	public void testDesiredAdvanceDuringCandidateBuildRetriesNewestGeneration() {
		FlowExecutor executor = loadVersionOne("THEN(a, b)");
		InMemoryRuleRepository.publishChain("chain1", "THEN(b, a)");
		RuleDbSyncManager.pollOnce();
		InMemoryRuleRepository.afterNextChainFetch(() -> {
			InMemoryRuleRepository.publishChain("chain1", "THEN(c, a)");
			RuleDbRuntime.applyChange(lastChange());
		});

		LiteflowResponse oldResponse = executor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(oldResponse.isSuccess());
		Assertions.assertEquals("a==>b", oldResponse.getExecuteStepStr());
		Assertions.assertEquals(RuleTargetStatus.STALE, RuleDbRuntime.chainState("chain1").getStatus());

		LiteflowResponse newestResponse = executor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(newestResponse.isSuccess());
		Assertions.assertEquals("c==>a", newestResponse.getExecuteStepStr());
		Assertions.assertEquals(3L, RuleDbRuntime.chainState("chain1").getActiveVersion());
	}

	@Test
	public void testDeleteDuringCandidateBuildCannotActivateFetchedVersion() {
		FlowExecutor executor = loadVersionOne("THEN(a, b)");
		InMemoryRuleRepository.publishChain("chain1", "THEN(b, a)");
		RuleDbSyncManager.pollOnce();
		InMemoryRuleRepository.afterNextChainFetch(() -> {
			InMemoryRuleRepository.deleteChain("chain1");
			RuleDbRuntime.applyChange(lastChange());
		});

		LiteflowResponse response = executor.execute2Resp("chain1", "arg");

		Assertions.assertFalse(response.isSuccess());
		Assertions.assertFalse(FlowBus.containChain("chain1"));
		RuleTargetState state = RuleDbRuntime.chainState("chain1");
		Assertions.assertEquals(1L, state.getActiveVersion());
		Assertions.assertEquals(RuleTargetStatus.DELETED, state.getStatus());
		assertNoCandidateChainPublished();
	}

	@Test
	public void testDeleteAndRecreateDuringCandidateBuildKeepsNewShadowIsolated() {
		FlowExecutor executor = loadVersionOne("THEN(a, b)");
		com.yomahub.liteflow.flow.element.Chain oldChain = FlowBus.getChain("chain1");
		InMemoryRuleRepository.publishChain("chain1", "THEN(b, a)");
		RuleDbSyncManager.pollOnce();
		InMemoryRuleRepository.afterNextChainFetch(() -> {
			InMemoryRuleRepository.deleteChain("chain1");
			RuleDbRuntime.applyChange(lastChange());
			InMemoryRuleRepository.publishChain("chain1", "THEN(c)");
			RuleDbRuntime.applyChange(lastChange());
		});

		LiteflowResponse staleResponse = executor.execute2Resp("chain1", "arg");

		Assertions.assertFalse(staleResponse.isSuccess());
		Assertions.assertNotSame(oldChain, FlowBus.getChain("chain1"));
		RuleTargetState recreated = RuleDbRuntime.chainState("chain1");
		Assertions.assertEquals(0L, recreated.getActiveVersion());
		Assertions.assertEquals(RuleTargetStatus.SHADOW, recreated.getStatus());

		LiteflowResponse recreatedResponse = executor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(recreatedResponse.isSuccess());
		Assertions.assertEquals("c", recreatedResponse.getExecuteStepStr());
		Assertions.assertEquals(1L, recreated.getActiveVersion());
		assertNoCandidateChainPublished();
	}

	@Test
	public void testCandidateSupportsLazySubChainAndScriptReferences() {
		InMemoryRuleRepository.putChain("chain1", "THEN(a)");
		InMemoryRuleRepository.putChain("sub1", "THEN(b)");
		InMemoryRuleRepository.putScript("s1", "defaultContext.setData(\"candidateScript\", true);",
				"script", "groovy");
		registerCommonCmp();
		FlowExecutor executor = buildExecutor(new RuleDbConfig());
		Assertions.assertTrue(executor.execute2Resp("chain1", "arg").isSuccess());

		InMemoryRuleRepository.publishChain("chain1", "THEN(sub1, s1, c)");
		RuleDbSyncManager.pollOnce();
		LiteflowResponse response = executor.execute2Resp("chain1", "arg");

		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("b==>s1==>c", response.getExecuteStepStr());
		Assertions.assertEquals(Boolean.TRUE,
				response.getContextBean(DefaultContext.class).getData("candidateScript"));
		Assertions.assertEquals(1L, RuleDbRuntime.chainState("sub1").getActiveVersion());
		assertNoCandidateChainPublished();
	}

	private FlowExecutor loadVersionOne(String el) {
		InMemoryRuleRepository.putChain("chain1", el);
		registerCommonCmp();
		FlowExecutor executor = buildExecutor(new RuleDbConfig());
		LiteflowResponse response = executor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals(1L, RuleDbRuntime.chainState("chain1").getActiveVersion());
		return executor;
	}

	private void registerRouteCmp() {
		LiteFlowNodeBuilder.createBooleanNode().setId("route").setClazz(RouteCmp.class).build();
	}

	private static void putRouteChain(String chainId, String el, String route, String namespace) {
		InMemoryRuleRepository.putChain(chainId, el, namespace);
		InMemoryRuleRepository.CHAINS.get(chainId).setRoute(route);
	}

	private static void publishRouteChain(String chainId, String el, String route, String namespace) {
		InMemoryRuleRepository.publishChain(chainId, el);
		InMemoryRuleRepository.CHAINS.get(chainId).setRoute(route);
		InMemoryRuleRepository.CHAINS.get(chainId).setNamespace(namespace);
	}

	private static String elMd5(String el) {
		return MD5.create().digestHex(el);
	}

	public static class RouteCmp extends NodeBooleanComponent {

		@Override
		public boolean processBoolean() {
			return Boolean.TRUE.equals(this.<Boolean>getRequestData());
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}

	private static com.yomahub.liteflow.repository.vo.ChangeRecord lastChange() {
		return InMemoryRuleRepository.CHANGES.get(InMemoryRuleRepository.CHANGES.size() - 1);
	}

	private static void assertNoCandidateChainPublished() {
		Assertions.assertTrue(FlowBus.getChainMap().keySet().stream()
				.noneMatch(chainId -> chainId.contains("@ruleDb@")));
	}
}
