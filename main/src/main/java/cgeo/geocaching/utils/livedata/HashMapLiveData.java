package cgeo.geocaching.utils.livedata;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.HashMap;

public class HashMapLiveData<K, V> extends LiveData<HashMapLiveData<K, V>.ChangeEvent> {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final HashMap<K, V> items = new HashMap<>();

    public enum ChangeEventType {
        ADD, REMOVE
    }

    public class ChangeEvent {
        private final ChangeEventType type;
        private final K key;

        private ChangeEvent(final ChangeEventType type, final K key) {
            this.type = type;
            this.key = key;
        }

        public K getKey() {
            return key;
        }

        public ChangeEventType getType() {
            return type;
        }
    }

    public void add(final K key, final V value) {
        items.put(key, value);
        handler.post(() -> setValue(new ChangeEvent(ChangeEventType.ADD, key)));
    }

    public V remove(final K key) {
        final V removed = items.remove(key);
        handler.post(() -> setValue(new ChangeEvent(ChangeEventType.REMOVE, key)));
        return removed;
    }

    public void observeAddEvents(final @NonNull LifecycleOwner owner, final @NonNull Observer<K> observer) {

        for (K key : items.keySet()) {
            observer.onChanged(key);
        }

        super.observe(owner, changeEvent -> {
            if (changeEvent.type == ChangeEventType.ADD) {
                observer.onChanged(changeEvent.getKey());
            }
        });
    }

    public void observeRemoveEvents(final @NonNull LifecycleOwner owner, final @NonNull Observer<K> observer) {
        super.observe(owner, changeEvent -> {
            if (changeEvent.type == ChangeEventType.REMOVE) {
                observer.onChanged(changeEvent.getKey());
            }
        });
    }

    public HashMap<K, V> getMap() {
        return items;
    }
}
