package com.zenvix.plugins;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PluginSystemTest {

    private PluginSystem pluginSystem;
    private PluginRegistry registry;
    private ZenviXContext context;

    @TempDir
    Path tempDir;

    public static class DummyPlugin implements ServicePlugin {
        private String id = java.util.UUID.randomUUID().toString();
        public boolean initialized = false;
        public boolean shutdown = false;

        public String getId() { return id; }
        public String getName() { return "Dummy"; }
        public String getVersion() { return "1.0"; }
        public String getDescription() { return "Desc"; }
        public void initialize(ZenviXContext context) { this.initialized = true; }
        public void start() {}
        public void stop() {}
        public ServiceStatus getStatus() { return ServiceStatus.RUNNING; }
        public javafx.scene.Node getControlPanelRow() { return null; }
        public void shutdown() { this.shutdown = true; }
    }

    @BeforeEach
    public void setUp() {
        context = new ZenviXContext(null, tempDir, tempDir);
        registry = new PluginRegistry();
        pluginSystem = new PluginSystem(context, registry);
    }

    @Test
    public void testLoad_loadsPluginFromJar() throws Exception {
        PluginSystem spySystem = spy(pluginSystem);
        
        Path jar1 = tempDir.resolve("test.jar");
        Files.createFile(jar1);
        
        Manifest mockManifest = new Manifest();
        mockManifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
        mockManifest.getMainAttributes().putValue("zenviX-Plugin-Class", "com.zenvix.plugins.PluginSystemTest$DummyPlugin");
        doReturn(mockManifest).when(spySystem).extractManifest(any(File.class));
        
        boolean loaded = spySystem.loadPlugin(jar1);
        
        assertTrue(loaded);
        assertEquals(1, registry.getAllPlugins().size());
        
        PluginRegistry.PluginMetadata metadata = registry.getAllPlugins().iterator().next();
        DummyPlugin plugin = (DummyPlugin) metadata.instance;
        assertTrue(plugin.initialized, "initialize() should be called");
    }

    @Test
    public void testIsolation_pluginsHaveSeparateClassLoaders() throws Exception {
        PluginSystem spySystem = spy(pluginSystem);
        
        Path jar1 = tempDir.resolve("test1.jar");
        Path jar2 = tempDir.resolve("test2.jar");
        Files.createFile(jar1);
        Files.createFile(jar2);
        
        Manifest mockManifest = new Manifest();
        mockManifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
        mockManifest.getMainAttributes().putValue("zenviX-Plugin-Class", "com.zenvix.plugins.PluginSystemTest$DummyPlugin");
        doReturn(mockManifest).when(spySystem).extractManifest(any(File.class));
        
        spySystem.loadPlugin(jar1);
        spySystem.loadPlugin(jar2);
        
        Object[] plugins = registry.getAllPlugins().toArray();
        assertEquals(2, plugins.length);
        
        PluginRegistry.PluginMetadata m1 = (PluginRegistry.PluginMetadata) plugins[0];
        PluginRegistry.PluginMetadata m2 = (PluginRegistry.PluginMetadata) plugins[1];
        
        assertNotSame(m1.classLoader, m2.classLoader, "Plugins must use isolated classloaders");
    }

    @Test
    public void testLifecycle_initializeStartStop() throws Exception {
        DummyPlugin plugin = new DummyPlugin();
        plugin.initialize(context);
        plugin.start();
        assertEquals(ServicePlugin.ServiceStatus.RUNNING, plugin.getStatus());
        plugin.stop();
        plugin.shutdown();
        assertTrue(plugin.shutdown);
    }

    @Test
    public void testRegistry_listLoadedPlugins() throws Exception {
        PluginSystem spySystem = spy(pluginSystem);
        Path jar1 = tempDir.resolve("test.jar");
        Files.createFile(jar1);
        
        Manifest mockManifest = new Manifest();
        mockManifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
        mockManifest.getMainAttributes().putValue("zenviX-Plugin-Class", "com.zenvix.plugins.PluginSystemTest$DummyPlugin");
        doReturn(mockManifest).when(spySystem).extractManifest(any(File.class));
        
        spySystem.loadPlugin(jar1);
        
        assertEquals(1, registry.getAllPlugins().size());
        assertEquals("Dummy", registry.getAllPlugins().iterator().next().name);
    }

    @Test
    public void testHotUnload_unloadsWithoutRestart() throws Exception {
        PluginSystem spySystem = spy(pluginSystem);
        Path jar1 = tempDir.resolve("test.jar");
        Files.createFile(jar1);
        
        Manifest mockManifest = new Manifest();
        mockManifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
        mockManifest.getMainAttributes().putValue("zenviX-Plugin-Class", "com.zenvix.plugins.PluginSystemTest$DummyPlugin");
        doReturn(mockManifest).when(spySystem).extractManifest(any(File.class));
        
        spySystem.loadPlugin(jar1);
        
        PluginRegistry.PluginMetadata meta = registry.getAllPlugins().iterator().next();
        String id = meta.id;
        DummyPlugin plugin = (DummyPlugin) meta.instance;
        
        boolean unloaded = spySystem.unloadPlugin(id);
        
        assertTrue(unloaded);
        assertEquals(0, registry.getAllPlugins().size());
        assertTrue(plugin.shutdown, "Unloading must trigger shutdown");
    }
}
