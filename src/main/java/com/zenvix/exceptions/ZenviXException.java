package com.zenvix.exceptions;

/**
 * Core Abstract exception explicitly demanding logical tracking fields 
 * securing physical debugging contexts intuitively.
 */
public class ZenviXException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final String userFriendlyMessage;
    private final String technicalDetails;
    private final String suggestedAction;

    public ZenviXException(ErrorCode errorCode, String userFriendlyMessage, String technicalDetails, String suggestedAction, Throwable cause) {
        super(userFriendlyMessage, cause);
        this.errorCode = errorCode;
        this.userFriendlyMessage = userFriendlyMessage;
        this.technicalDetails = technicalDetails;
        this.suggestedAction = suggestedAction;
    }

    public ZenviXException(ErrorCode errorCode, String userFriendlyMessage, String technicalDetails, String suggestedAction) {
        this(errorCode, userFriendlyMessage, technicalDetails, suggestedAction, null);
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public String getUserFriendlyMessage() { return userFriendlyMessage; }
    public String getTechnicalDetails() { return technicalDetails; }
    public String getSuggestedAction() { return suggestedAction; }
}
