/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.yomahub.liteflow.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A basic copy on write HashMap.
 * <p>
 * If an instance is cloned then any methods invoked on the instance or clone that result
 * in state modification will result in copying of the state before modification.
 *
 * @author Paul.Sandoz@Oracle.Com
 * @author pavel.bucek@oracle.com
 * @author Bryan.Zhang
 */
public class CopyOnWriteHashMap<K, V> extends ConcurrentHashMap<K, V> {

	volatile ConcurrentHashMap<K, V> view;

	private ConcurrentHashMap<K, V> duplicate(ConcurrentHashMap<K, V> original) {
		// SUBTLETY: note that original.entrySet() grabs the entire contents of the
		// original Map in a
		// single call. This means that if the original map is Thread-safe or another
		// CopyOnWriteHashMap,
		// we can safely iterate over the list of entries.
		return new ConcurrentHashMap<>(original);
	}

	public CopyOnWriteHashMap(ConcurrentHashMap<K, V> that) {
		this.view = duplicate(that);
	}

	public CopyOnWriteHashMap() {
		this(new ConcurrentHashMap<>());
	}

	@Override
	public CopyOnWriteHashMap<K, V> clone() {
		return new CopyOnWriteHashMap(view);
	}

	/*
	 * ********************** READ-ONLY OPERATIONS
	 **********************/

	@Override
	public int size() {
		return view.size();
	}

	@Override
	public boolean isEmpty() {
		return view.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return view.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return view.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return view.get(key);
	}

	@Override
	public KeySetView<K, V> keySet() {
		return view.keySet();
	}

	@Override
	public Collection<V> values() {
		return view.values();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return view.entrySet();
	}

	@Override
	public String toString() {
		return view.toString();
	}

	/*
	 * ********************** UPDATING OPERATIONS
	 *
	 * These operations all follow a common pattern:
	 *
	 * 1. Create a copy of the existing view. 2. Update the copy. 3. Perform a volatile
	 * write to replace the existing view.
	 *
	 * Note that if you are not concerned about lost updates, you could dispense with the
	 * synchronization entirely.
	 **********************/

	@Override
	public V put(K key, V value) {
		synchronized (this) {
			ConcurrentHashMap<K, V> newCore = duplicate(view);
			V result = newCore.put(key, value);
			view = newCore; // volatile write
			return result;
		}
	}

	@Override
	public V remove(Object key) {
		synchronized (this) {
			ConcurrentHashMap<K, V> newCore = duplicate(view);
			V result = newCore.remove(key);
			view = newCore; // volatile write
			return result;
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> t) {
		synchronized (this) {
			ConcurrentHashMap<K, V> newCore = duplicate(view);
			newCore.putAll(t);
			view = newCore; // volatile write
		}
	}

	@Override
	public void clear() {
		synchronized (this) {
			view = new ConcurrentHashMap<>(); // volatile write
		}
	}

}