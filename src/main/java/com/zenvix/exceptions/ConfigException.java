package com.zenvix.exceptions;

public class ConfigException extends ZenviXException {
    public ConfigException(ErrorCode code, String msg, String tech, String action) { super(code, msg, tech, action); }

    public static class ConfigCorruptException extends ConfigException {
        public ConfigCorruptException(String msg, String tech, String action) { super(ErrorCode.CONFIG_CORRUPT, msg, tech, action); }
    }

    public static class ConfigMigrationException extends ConfigException {
        public final int fromVersion;
        public final int toVersion;
        public ConfigMigrationException(String msg, String tech, String action, int from, int to) {
            super(ErrorCode.CONFIG_SCHEMA_MISMATCH, msg, tech, action);
            this.fromVersion = from;
            this.toVersion = to;
        }
    }
}
