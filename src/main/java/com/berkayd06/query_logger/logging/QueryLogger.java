package com.berkayd06.query_logger.logging;

public interface QueryLogger {
    void logQuery(String loggerName, String origin, double durationMs, boolean canceled, 
                  String sql, String params);
    
    void logQueryWithoutParams(String loggerName, String origin, double durationMs, 
                               boolean canceled, String sql);
    
    void logQueryError(String loggerName, String origin, double durationMs, boolean canceled,
                       String sql, String exceptionType, String errorMsg, String params);
    
    void logQueryErrorWithoutParams(String loggerName, String origin, double durationMs, 
                                    boolean canceled, String sql, String exceptionType, 
                                    String errorMsg);
    
    void logWarning(String message);
    
    void logInfo(String message);
    
    void logError(String message);
}
