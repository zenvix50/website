package com.zenvix.plugins;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.jar.Manifest;

/**
 * Automates hot-swappable Dynamic URLClassLoader injections natively mapping specific 
 * Jar boundaries effectively ensuring strong isolation pipelines successfully securely!
 */
public class PluginSystem {

    private final Path pluginsDir = Paths.get("zenvix", "plugins");
    private final PluginRegistry registry;
    private final ZenviXContext context;

    public PluginSystem(ZenviXContext context, PluginRegistry registry) {
        this.context = context;
        this.registry = registry;
        try {
            Files.createDirectories(pluginsDir);
        } catch (IOException e) {}
    }

    public void scanAndLoadPlugins() {
        File[] files = pluginsDir.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return;
        
        for (File file : files) {
            loadPlugin(file.toPath());
        }
    }

    protected Manifest extractManifest(File jarFile) throws IOException {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            return jar.getManifest();
        }
    }

    public boolean loadPlugin(Path jarPath) {
        try {
            URL jarUrl = jarPath.toUri().toURL();
            URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, PluginSystem.class.getClassLoader());
            
            Manifest manifest = extractManifest(jarPath.toFile());
            if (manifest == null) return false;
            
            String mainClass = manifest.getMainAttributes().getValue("zenviX-Plugin-Class");
            String author = manifest.getMainAttributes().getValue("zenviX-Plugin-Author");
            if (mainClass == null) return false;
            
            Class<?> clazz = classLoader.loadClass(mainClass);
            ServicePlugin plugin = (ServicePlugin) clazz.getDeclaredConstructor().newInstance();
            
            plugin.initialize(context);
            
            PluginRegistry.PluginMetadata metadata = new PluginRegistry.PluginMetadata(plugin, classLoader, author != null ? author : "Unknown");
            registry.register(metadata);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean installPlugin(Path sourceJarPath) {
        try {
            Path destPath = pluginsDir.resolve(sourceJarPath.getFileName());
            Files.copy(sourceJarPath, destPath, StandardCopyOption.REPLACE_EXISTING);
            return loadPlugin(destPath);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean unloadPlugin(String pluginId) {
        PluginRegistry.PluginMetadata metadata = registry.unregister(pluginId);
        if (metadata != null) {
            try {
                metadata.instance.shutdown();
                metadata.classLoader.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public void shutdownAll() {
        for (PluginRegistry.PluginMetadata meta : registry.getAllPlugins()) {
            try {
                meta.instance.shutdown();
                meta.classLoader.close();
            } catch (Exception e) {}
        }
        registry.clear();
    }
}
