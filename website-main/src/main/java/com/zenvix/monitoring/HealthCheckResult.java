package com.zenvix.monitoring;

/**
 * Encapsulates exact Socket execution returns capturing success states 
 * securely merging detailed diagnostic string representations natively.
 */
public class HealthCheckResult {
    public final boolean isSuccess;
    public final String message;

    public HealthCheckResult(boolean isSuccess, String message) {
        this.isSuccess = isSuccess;
        this.message = message;
    }
}
