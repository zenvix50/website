package com.zenvix.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    @TempDir
    Path tempDir;

    private GlobalExceptionHandler handler;

    @BeforeEach
    public void setUp() throws Exception {
        handler = spy(new GlobalExceptionHandler());
        
        java.lang.reflect.Field logDirField = GlobalExceptionHandler.class.getDeclaredField("logDir");
        logDirField.setAccessible(true);
        logDirField.set(handler, tempDir);
        
        doNothing().when(handler).showUserDialog(any(), any());
    }

    @Test
    public void testWriteCrashReport_handlesZenviXException() throws Exception {
        ZenviXException ex = new ZenviXException(ErrorCode.PORT_CONFLICT, "Port busy", "Port 8080 busy", "Kill process");
        Path crashFile = handler.writeCrashReport(Thread.currentThread(), ex);
        
        assertNotNull(crashFile);
        assertTrue(Files.exists(crashFile));
        
        String content = Files.readString(crashFile);
        assertTrue(content.contains("PORT_CONFLICT"));
        assertTrue(content.contains("Port busy"));
        assertTrue(content.contains("Kill process"));
        assertTrue(content.contains("JVM Version"));
    }

    @Test
    public void testWriteCrashReport_handlesStandardException() throws Exception {
        RuntimeException ex = new RuntimeException("Null pointer simulated");
        Path crashFile = handler.writeCrashReport(Thread.currentThread(), ex);
        
        assertNotNull(crashFile);
        assertTrue(Files.exists(crashFile));
        
        String content = Files.readString(crashFile);
        assertTrue(content.contains("Null pointer simulated"));
        assertTrue(content.contains("--- Stack Trace ---"));
    }
    
    @Test
    public void testRegister_setsDefaultHandler() {
        handler.register();
        assertEquals(handler, Thread.getDefaultUncaughtExceptionHandler());
    }
}
