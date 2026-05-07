package com.zenvix.exceptions;

public class NetworkException extends ZenviXException {
    public NetworkException(ErrorCode code, String msg, String tech, String action) { super(code, msg, tech, action); }

    public static class OfflineException extends NetworkException {
        public OfflineException(String msg, String tech, String action) { super(ErrorCode.OFFLINE, msg, tech, action); }
    }
}
