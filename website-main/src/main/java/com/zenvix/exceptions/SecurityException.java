package com.zenvix.exceptions;

public class SecurityException extends ZenviXException {
    public SecurityException(ErrorCode code, String msg, String tech, String action) { super(code, msg, tech, action); }

    public static class VaultException extends SecurityException {
        public VaultException(ErrorCode code, String msg, String tech, String action) { super(code, msg, tech, action); }
    }

    public static class CertificateException extends SecurityException {
        public CertificateException(ErrorCode code, String msg, String tech, String action) { super(code, msg, tech, action); }
    }
}
