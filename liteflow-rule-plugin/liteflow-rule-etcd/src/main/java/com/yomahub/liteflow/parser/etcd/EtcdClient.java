package com.yomahub.liteflow.parser.etcd;

import cn.hutool.core.collection.CollUtil;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.watch.WatchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Etcd 客户端封装类.
 * @author zendwang
 * @since 2.9.0
 */
public class EtcdClient {

	private static final Logger LOG = LoggerFactory.getLogger(EtcdClient.class);

	private final Client client;

	private final ConcurrentHashMap<String, Watch.Watcher> watchCache = new ConcurrentHashMap<>();

	public EtcdClient(final Client client) {
		this.client = client;
	}

	/**
	 * close client.
	 */
	public void close() {
		this.client.close();
	}

	/**
	 * get node value.
	 *
	 * @param key node name
	 * @return string
	 */
	public String get(final String key) {
		List<KeyValue> keyValues = null;
		try {
			keyValues = client.getKVClient().get(ByteSequence.from(key, StandardCharsets.UTF_8)).get().getKvs();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error(e.getMessage(), e);
		}

		if (CollUtil.isEmpty(keyValues)) {
			return null;
		}

		return keyValues.iterator().next().getValue().toString(StandardCharsets.UTF_8);
	}

	/**
	 *  put a key-value pair into etcd.
	 * @param key node name
	 * @param value node value
	 * @return
	 */
	public KeyValue put(final String key, final String value) {
		KeyValue prevKv = null;
		ByteSequence keyByteSequence = ByteSequence.from(key, StandardCharsets.UTF_8);
		ByteSequence valueByteSequence = ByteSequence.from(value, StandardCharsets.UTF_8);
		try {
			prevKv = client.getKVClient().put(keyByteSequence, valueByteSequence).get().getPrevKv();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error(e.getMessage(), e);
		}
		return prevKv;
	}

	/**
	 * subscribe data change.
	 *
	 * @param key           node name
	 * @param updateHandler node value handler of update
	 * @param deleteHandler node value handler of delete
	 */
	public void watchDataChange(final String key,
	                            final BiConsumer<String, String> updateHandler,
	                            final Consumer<String> deleteHandler) {
		Watch.Listener listener = watch(updateHandler, deleteHandler);
		Watch.Watcher watch = client.getWatchClient().watch(ByteSequence.from(key, StandardCharsets.UTF_8), listener);
		watchCache.put(key, watch);
	}

	private Watch.Listener watch(final BiConsumer<String, String> updateHandler,
	                             final Consumer<String> deleteHandler) {
		return Watch.listener(response -> {
			for (WatchEvent event : response.getEvents()) {
				String path = event.getKeyValue().getKey().toString(StandardCharsets.UTF_8);
				String value = event.getKeyValue().getValue().toString(StandardCharsets.UTF_8);
				switch (event.getEventType()) {
					case PUT:
						updateHandler.accept(path, value);
						continue;
					case DELETE:
						deleteHandler.accept(path);
						continue;
					default:
				}
			}
		});
	}

	/**
	 * cancel subscribe.
	 *
	 * @param key node name
	 */
	public void watchClose(final String key) {
		if (watchCache.containsKey(key)) {
			watchCache.get(key).close();
			watchCache.remove(key);
		}
	}
}