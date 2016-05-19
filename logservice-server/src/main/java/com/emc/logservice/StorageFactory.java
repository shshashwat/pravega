package com.emc.logservice;

/**
 * Defines a Factory for Storage Adapters.
 */
public interface StorageFactory {
    /**
     * Gets a reference to an instance of a Storage class.
     *
     * @return
     */
    Storage getStorageAdapter();
}
