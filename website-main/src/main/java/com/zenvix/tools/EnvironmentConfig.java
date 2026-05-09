package com.zenvix.tools;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * POJO representing the environment configurations globally and per-project natively in zenviX.
 */
public class EnvironmentConfig {
    public final Map<String, String> global = new ConcurrentHashMap<>();
    public final Map<String, Map<String, String>> projects = new ConcurrentHashMap<>();
}
