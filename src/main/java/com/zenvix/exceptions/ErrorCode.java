package com.zenvix.exceptions;

/**
 * Global standardized ErrorCodes natively identifying explicit crash 
 * contexts automatically bypassing raw StackTrace ambiguities effortlessly.
 */
public enum ErrorCode {
    // Service errors
    SERVICE_START_FAILED, SERVICE_STOP_FAILED, SERVICE_ALREADY_RUNNING, SERVICE_NOT_FOUND,
    PORT_CONFLICT, PROCESS_KILL_FAILED, BINARY_NOT_FOUND, BINARY_VERSION_INCOMPATIBLE,

    // Config errors
    CONFIG_NOT_FOUND, CONFIG_CORRUPT, CONFIG_SCHEMA_MISMATCH, CONFIG_WRITE_FAILED, CONFIG_VALIDATION_FAILED,

    // Security errors
    VAULT_LOCKED, VAULT_CORRUPT, VAULT_WRONG_PASSWORD, CERT_GENERATION_FAILED, CERT_EXPIRED,

    // Network errors
    PORT_UNAVAILABLE, CONNECTION_REFUSED, TIMEOUT, OFFLINE,

    // System errors
    INSUFFICIENT_PERMISSIONS, DISK_FULL, OS_NOT_SUPPORTED, PLUGIN_LOAD_FAILED
}
