package com.zenvix.exceptions;

public class ServiceException extends ZenviXException {
    public ServiceException(ErrorCode code, String msg, String tech, String action) { super(code, msg, tech, action); }
    public ServiceException(ErrorCode code, String msg, String tech, String action, Throwable cause) { super(code, msg, tech, action, cause); }

    public static class ServiceStartException extends ServiceException {
        public ServiceStartException(String msg, String tech, String action) { super(ErrorCode.SERVICE_START_FAILED, msg, tech, action); }
    }
    
    public static class ServiceStopException extends ServiceException {
        public ServiceStopException(String msg, String tech, String action) { super(ErrorCode.SERVICE_STOP_FAILED, msg, tech, action); }
    }
    
    public static class PortConflictException extends ServiceException {
        public final int conflictingPort;
        public final String occupyingProcess;
        public final int occupyingPid;

        public PortConflictException(String msg, String tech, String action, int port, String process, int pid) {
            super(ErrorCode.PORT_CONFLICT, msg, tech, action);
            this.conflictingPort = port;
            this.occupyingProcess = process;
            this.occupyingPid = pid;
        }
    }
}
