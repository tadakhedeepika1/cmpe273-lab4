package edu.sjsu.cmpe.cache.client;

/**
 * Cache Service Interface
 * 
 */
public interface CacheInterface {
    public String get(long key);

    public void put(long key, String value);

    /* Added Delete */
    void delete(long key);
}
