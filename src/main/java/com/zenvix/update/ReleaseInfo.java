package com.zenvix.update;

/**
 * Encapsulates immutable physical release boundaries mapped from GitHub API 
 * validating semantic versions securely natively avoiding object drift dynamically.
 */
public class ReleaseInfo {
    public final String version;
    public final String body;
    public final String downloadUrl;
    public final String assetName;

    public ReleaseInfo(String version, String body, String downloadUrl, String assetName) {
        this.version = version;
        this.body = body;
        this.downloadUrl = downloadUrl;
        this.assetName = assetName;
    }
}
