package com.zenvix.setup;

/**
 * Enumerates the strictly enforced chronological checkpoints required during 
 * the Initial Bootstrap phase naturally securing setup integrity synchronously.
 */
public enum WizardStep {
    WELCOME,
    DETECT_INSTALLATIONS,
    CONFIGURE_PATHS,
    MASTER_PASSWORD,
    SELECT_SERVICES,
    COMPLETE
}
