package com.yomahub.liteflow.repository.runtime;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.exception.ChainLoadException;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.repository.RuleRepository;
import com.yomahub.liteflow.repository.vo.ChainMeta;
import com.yomahub.liteflow.repository.vo.ChainRecord;
import com.yomahub.liteflow.util.ElFingerprint;

import java.util.Objects;
import java.util.UUID;

/** Loads and compiles one stable chain generation without publishing it to FlowBus. */
public final class ChainCandidateLoader {

	private final RuleRepository repository;
	private final int retryTimes;

	public ChainCandidateLoader(RuleRepository repository, int retryTimes) {
		this.repository = repository;
		this.retryTimes = retryTimes;
	}

	public Candidate load(String chainId, long expectedVersion, String expectedMd5) {
		ChainMeta before = fetchMeta(chainId);
		assertExpectedGeneration(chainId, before, expectedVersion, expectedMd5);

		ChainRecord record = fetchChain(chainId);
		assertContentGeneration(chainId, before, record);

		ChainMeta after = fetchMeta(chainId);
		if (!sameGeneration(before, after)) {
			throw changed(chainId);
		}

		String candidateId = chainId + "@ruleDb@" + expectedVersion + "@" + UUID.randomUUID();
		Chain candidate = new Chain(candidateId);
		candidate.setEl(record.getEl());
		candidate.setElMd5(ElFingerprint.of(record.getEl()));
		candidate.setRouteEl(record.getRoute());
		if (StrUtil.isNotBlank(record.getNamespace())) {
			candidate.setNamespace(record.getNamespace());
		}
		LiteFlowChainELBuilder.compileUnpublishedChain(candidate, chainId);
		return new Candidate(candidate, record.getVersion(), record.getMd5());
	}

	private void assertExpectedGeneration(String chainId, ChainMeta meta, long version, String md5) {
		if (meta == null || meta.getVersion() != version
				|| (md5 != null && !Objects.equals(md5, meta.getMd5()))) {
			throw changed(chainId);
		}
	}

	private void assertContentGeneration(String chainId, ChainMeta meta, ChainRecord record) {
		if (record == null || !record.isEnable() || !chainId.equals(record.getChainId())
				|| record.getVersion() != meta.getVersion()
				|| !Objects.equals(record.getMd5(), meta.getMd5())) {
			throw changed(chainId);
		}
	}

	private boolean sameGeneration(ChainMeta left, ChainMeta right) {
		return left != null && right != null
				&& left.getVersion() == right.getVersion()
				&& Objects.equals(left.getMd5(), right.getMd5());
	}

	private ChainMeta fetchMeta(String chainId) {
		RuntimeException last = null;
		for (int i = 0; i <= retryTimes; i++) {
			try {
				return repository.fetchChainMeta(chainId);
			} catch (RuntimeException e) {
				last = e;
			}
		}
		throw fetchFailed("metadata", chainId, last);
	}

	private ChainRecord fetchChain(String chainId) {
		RuntimeException last = null;
		for (int i = 0; i <= retryTimes; i++) {
			try {
				return repository.fetchChain(chainId);
			} catch (RuntimeException e) {
				last = e;
			}
		}
		throw fetchFailed("content", chainId, last);
	}

	private ChainLoadException fetchFailed(String part, String chainId, RuntimeException error) {
		return new ChainLoadException(StrUtil.format("fetch chain {}[{}] failed after {} retries: {}",
				part, chainId, retryTimes, error == null ? StrUtil.EMPTY : error.getMessage()));
	}

	private ChainLoadException changed(String chainId) {
		return new ChainLoadException(StrUtil.format("chain[{}] changed or was deleted while loading", chainId));
	}

	public static final class Candidate {

		private final Chain chain;
		private final long version;
		private final String md5;

		private Candidate(Chain chain, long version, String md5) {
			this.chain = chain;
			this.version = version;
			this.md5 = md5;
		}

		public Chain getChain() {
			return chain;
		}

		public long getVersion() {
			return version;
		}

		public String getMd5() {
			return md5;
		}
	}
}
