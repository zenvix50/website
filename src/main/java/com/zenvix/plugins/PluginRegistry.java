package com.zenvix.plugins;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe Storage Map enforcing unique Registry tracking inherently mapping 
 * URLClassLoaders and Plugin references synchronously directly avoiding corruptions!
 */
public class PluginRegistry {

    public static class PluginMetadata {
        public final String id;
        public final String name;
        public final String version;
        public final String author;
        public final String description;
        public boolean enabled = true;
        public final ServicePlugin instance;
        public final java.net.URLClassLoader classLoader;

        public PluginMetadata(ServicePlugin instance, java.net.URLClassLoader classLoader, String author) {
            this.id = instance.getId();
            this.name = instance.getName();
            this.version = instance.getVersion();
            this.description = instance.getDescription();
            this.instance = instance;
            this.classLoader = classLoader;
            this.author = author;
        }
    }

    private final Map<String, PluginMetadata> registry = new ConcurrentHashMap<>();

    public void register(PluginMetadata metadata) {
        registry.put(metadata.id, metadata);
    }

    public PluginMetadata unregister(String id) {
        return registry.remove(id);
    }

    public PluginMetadata get(String id) {
        return registry.get(id);
    }

    public Collection<PluginMetadata> getAllPlugins() {
        return registry.values();
    }
    
    public void clear() {
        registry.clear();
    }
}
