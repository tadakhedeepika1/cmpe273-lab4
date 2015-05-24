package edu.sjsu.cmpe.cache.api.api.repository;

import edu.sjsu.cmpe.cache.api.api.domain.Entry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class InMemoryCache implements CacheInterface {
    /** In-memory map cache. (Key, Value) -> (Key, Entry) */
    private final ConcurrentHashMap<Long, Entry> inMemoryMap;

    public InMemoryCache(ConcurrentHashMap<Long, Entry> entries) {
        inMemoryMap = entries;
    }

    @Override
    public Entry save(Entry newEntry) {
        checkNotNull(newEntry, "new Entry instance must not be null");
        inMemoryMap.put(newEntry.getKey(), newEntry);
        Entry newValue = inMemoryMap.get(newEntry.getKey());
        System.out.println("inserted" + newEntry.getValue());
        System.out.println("cached" + newValue.getValue());

        return newEntry;
    }

    @Override
    public Entry get(Long key) {
        checkArgument(key > 0,
                "Invalid Key", key);
        return inMemoryMap.get(key);
    }

    @Override
    public void delete(Long key) {
        checkArgument(key > 0,
                "Invalid Key", key);
        inMemoryMap.remove(key);
    }

    @Override
    public List<Entry> getAll() {
        return new ArrayList<Entry>(inMemoryMap.values());
    }
}
