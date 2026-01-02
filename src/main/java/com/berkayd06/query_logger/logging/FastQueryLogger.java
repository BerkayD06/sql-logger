package com.berkayd06.query_logger.logging;

import java.io.PrintStream;

public final class FastQueryLogger implements QueryLogger {
    
    private static final String INFO_PREFIX = "INFO  ";
    private static final String WARN_PREFIX = "WARN  ";
    private static final String ERROR_PREFIX = "ERROR ";
    private static final String EVENT_QUERY = " - event=sql_query origin=";
    private static final String DURATION_MS = " duration_ms=";
    private static final String CANCELED = " canceled=";
    private static final String SQL = " sql=\"";
    private static final String PARAMS = " params=";
    private static final String EXCEPTION = " ex=";
    private static final String MSG = " msg=\"";
    private static final String QUOTE = "\"";
    
    private final PrintStream out;
    private final PrintStream err;
    
    public FastQueryLogger() {
        this(System.out, System.err);
    }
    
    public FastQueryLogger(PrintStream out) {
        this(out, System.err);
    }
    
    public FastQueryLogger(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }
    
    @Override
    public void logQuery(String loggerName, String origin, double durationMs, boolean canceled, 
                        String sql, String params) {
        try {
            StringBuilder sb = new StringBuilder(256);
            sb.append(INFO_PREFIX)
              .append(loggerName)
              .append(EVENT_QUERY)
              .append(origin)
              .append(DURATION_MS);
            appendDouble(sb, durationMs);
            sb.append(CANCELED)
              .append(canceled)
              .append(SQL)
              .append(sql)
              .append(QUOTE)
              .append(PARAMS)
              .append(params);
            out.println(sb.toString());
        } catch (Exception e) {
        }
    }
    
    @Override
    public void logQueryWithoutParams(String loggerName, String origin, double durationMs, 
                                     boolean canceled, String sql) {
        try {
            StringBuilder sb = new StringBuilder(256);
            sb.append(INFO_PREFIX)
              .append(loggerName)
              .append(EVENT_QUERY)
              .append(origin)
              .append(DURATION_MS);
            appendDouble(sb, durationMs);
            sb.append(CANCELED)
              .append(canceled)
              .append(SQL)
              .append(sql)
              .append(QUOTE);
            out.println(sb.toString());
        } catch (Exception e) {
        }
    }
    
    @Override
    public void logQueryError(String loggerName, String origin, double durationMs, boolean canceled,
                             String sql, String exceptionType, String errorMsg, String params) {
        try {
            StringBuilder sb = new StringBuilder(384);
            sb.append(ERROR_PREFIX)
              .append(loggerName)
              .append(EVENT_QUERY)
              .append(origin)
              .append(DURATION_MS);
            appendDouble(sb, durationMs);
            sb.append(CANCELED)
              .append(canceled)
              .append(SQL)
              .append(sql)
              .append(QUOTE)
              .append(EXCEPTION)
              .append(exceptionType)
              .append(MSG)
              .append(errorMsg)
              .append(QUOTE)
              .append(PARAMS)
              .append(params);
            err.println(sb.toString());
        } catch (Exception e) {
        }
    }
    
    @Override
    public void logQueryErrorWithoutParams(String loggerName, String origin, double durationMs, 
                                          boolean canceled, String sql, String exceptionType, 
                                          String errorMsg) {
        try {
            StringBuilder sb = new StringBuilder(384);
            sb.append(ERROR_PREFIX)
              .append(loggerName)
              .append(EVENT_QUERY)
              .append(origin)
              .append(DURATION_MS);
            appendDouble(sb, durationMs);
            sb.append(CANCELED)
              .append(canceled)
              .append(SQL)
              .append(sql)
              .append(QUOTE)
              .append(EXCEPTION)
              .append(exceptionType)
              .append(MSG)
              .append(errorMsg)
              .append(QUOTE);
            err.println(sb.toString());
        } catch (Exception e) {
        }
    }
    
    @Override
    public void logWarning(String message) {
        try {
            err.print(WARN_PREFIX);
            err.println(message);
        } catch (Exception e) {
        }
    }
    
    @Override
    public void logInfo(String message) {
        try {
            out.print(INFO_PREFIX);
            out.println(message);
        } catch (Exception e) {
        }
    }
    
    @Override
    public void logError(String message) {
        try {
            err.print(ERROR_PREFIX);
            err.println(message);
        } catch (Exception e) {
        }
    }
    
    private static void appendDouble(StringBuilder sb, double value) {
        long intPart = (long) value;
        int fracPart = (int) ((value - intPart) * 1000);
        
        sb.append(intPart);
        sb.append('.');
        
        if (fracPart < 100) {
            sb.append('0');
            if (fracPart < 10) {
                sb.append('0');
            }
        }
        sb.append(fracPart);
    }
}
