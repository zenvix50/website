package com.zenvix.core.services;

import java.util.HashMap;
import java.util.Map;

/**
 * SecretsVault manages sensitive credentials for zenviX services.
 * Ensures passwords are never stored in plaintext config files.
 */
public class SecretsVault {
    private static final Map<String, String> vault = new HashMap<>();

    public static String get(String key) {
        return vault.get(key);
    }

    public static void set(String key, String value) {
        vault.put(key, value);
    }

    public static boolean has(String key) {
        return vault.containsKey(key);
    }
    
    public static void delete(String key) {
        vault.remove(key);
    }
    
    public static void clear() {
        vault.clear();
    }
}
