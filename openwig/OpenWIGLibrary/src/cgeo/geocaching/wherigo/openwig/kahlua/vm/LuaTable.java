/*
 Copyright (c) 2009 Kristofer Karlsson <kristofer.karlsson@gmail.com>

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

package cgeo.geocaching.wherigo.openwig.kahlua.vm;

import java.util.Map;
import cgeo.geocaching.wherigo.openwig.util.EntryIterator;

public interface LuaTable<K,V> extends Iterable<Map.Entry<K,V>> {
	default void setMetatable(LuaTable<? extends K, ? extends V> metatable) {
        for (Map.Entry<? extends K, ? extends V> e : metatable) {
            rawset(e.getKey(), e.getValue());
        }
    }
	LuaTable<K, V> getMetatable();

	void rawset(K key, V value);
	V rawget(K key);

	int len();

    default void rawset(Map.Entry<K,V> entry) {
        rawset(entry.getKey(), entry.getValue());
    }
    
    default K next(K key) {
        return null;
    }

    default java.util.Iterator<Map.Entry<K,V>> iterator() {
        return new EntryIterator<>(this);
    }
}
