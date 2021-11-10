/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A basic copy on write HashMap.
 * <p>
 * If an instance is cloned then any methods invoked on the instance or clone
 * that result in state modification will result in copying of the state
 * before modification.
 *
 * @author Paul.Sandoz@Oracle.Com
 * @author pavel.bucek@oracle.com
 */
public class CopyOnWriteHashMap<K,V> implements Map<K,V> {
    private volatile Map<K,V> core;

    volatile Map<K,V> view;

    private final AtomicBoolean requiresCopyOnWrite;

    public CopyOnWriteHashMap() {
        this.core = new HashMap<K, V>();
        this.requiresCopyOnWrite = new AtomicBoolean(false);
    }

    private CopyOnWriteHashMap(CopyOnWriteHashMap<K,V> that) {
        this.core = that.core;
        this.requiresCopyOnWrite = new AtomicBoolean(true);
    }

    @Override
    public CopyOnWriteHashMap<K,V> clone() {
        try {
            return new CopyOnWriteHashMap(this);
        } finally {
            requiresCopyOnWrite.set(true);
        }
    }

    private void copy() {
        if (requiresCopyOnWrite.compareAndSet(true, false)) {
            core = new HashMap<K, V>(core);
            view = null;
        }
    }

    @Override
    public int size() {
        return core.size();
    }

    @Override
    public boolean isEmpty() {
        return core.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return core.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return core.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return core.get(key);
    }

    @Override
    public V put(K key, V value) {
        copy();
        return core.put(key, value);
    }

    @Override
    public V remove(Object key) {
        copy();
        return core.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> t) {
        copy();
        core.putAll(t);
    }

    @Override
    public void clear() {
        core = new HashMap<K, V>();
        view = null;
        copy();
    }

    @Override
    public Set<K> keySet() {
        return getView().keySet();
    }

    @Override
    public Collection<V> values() {
        return getView().values();
    }

    @Override
    public Set<Entry<K,V>> entrySet() {
        return getView().entrySet();
    }

    @Override
    public String toString() {
        return core.toString();
    }

    private Map<K, V> getView() {
        Map<K, V> result = view; // volatile read
        if (result == null) {
            result = Collections.unmodifiableMap(core);
            view = result; // volatile write
        }
        return result;
    }
}
