package com.zenvix.core.services;

/**
 * Bounds precise physical boundaries resolving exceptions into readable UI 
 * prompts strictly distinguishing Start bounds from generic Failures dynamically.
 */
public class ZenviXException extends Exception {
    public enum ErrorCode { PORT_IN_USE, BINARY_NOT_FOUND, PROCESS_FAILED, TIMEOUT }
    
    public final ErrorCode errorCode;
    public final String technicalDetails;
    public final String suggestedAction;

    public ZenviXException(ErrorCode errorCode, String userFriendlyMessage, String technicalDetails, String suggestedAction) {
        super(userFriendlyMessage);
        this.errorCode = errorCode;
        this.technicalDetails = technicalDetails;
        this.suggestedAction = suggestedAction;
    }

    public static class ServiceException extends ZenviXException {
        public ServiceException(ErrorCode code, String msg, String tech, String action) { super(code, msg, tech, action); }
    }

    public static class ServiceStartException extends ServiceException {
        public ServiceStartException(String msg, String tech, String action) { super(ErrorCode.PROCESS_FAILED, msg, tech, action); }
    }

    public static class ServiceStopException extends ServiceException {
        public ServiceStopException(String msg, String tech, String action) { super(ErrorCode.PROCESS_FAILED, msg, tech, action); }
    }

    public static class PortConflictException extends ServiceException {
        public PortConflictException(String msg, String tech, String action) { super(ErrorCode.PORT_IN_USE, msg, tech, action); }
    }
}
