package com.berkayd06.query_logger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("querylogger")

public class QueryLoggerProperties {
    private boolean enabled = true;

    private long logThresholdMs = 200;
    private long cancelThresholdMs = 0;

    private boolean cancelEnabled = false;
    private boolean onlySlow = true;
    private boolean logParams = true;
    private LogFormat logFormat = LogFormat.FORMATTED;
    private double sampleRate = 1.0;
    private String loggerName = "com.berkayd06.querylogger";
    private String excludeSqlRegex;
    public enum LogFormat {
        SQL,
        FORMATTED,
        BOUND
    }

    public static class Vendor {
        public static class Postgresql {
            private long statementTimeoutMs = 0;
            public long getStatementTimeoutMs() { return statementTimeoutMs; }
            public void setStatementTimeoutMs(long statementTimeoutMs) { this.statementTimeoutMs = statementTimeoutMs; }
        }
        public static class Mysql {
            private long maxExecutionTimeMs = 0;
            public long getMaxExecutionTimeMs() { return maxExecutionTimeMs; }
            public void setMaxExecutionTimeMs(long maxExecutionTimeMs) { this.maxExecutionTimeMs = maxExecutionTimeMs; }
        }
        public static class Oracle {
            private long statementTimeoutMs = 0;
            public long getStatementTimeoutMs() { return statementTimeoutMs; }
            public void setStatementTimeoutMs(long statementTimeoutMs) { this.statementTimeoutMs = statementTimeoutMs; }
        }
        public static class SqlServer {
            private long queryTimeoutMs = 0;
            public long getQueryTimeoutMs() { return queryTimeoutMs; }
            public void setQueryTimeoutMs(long queryTimeoutMs) { this.queryTimeoutMs = queryTimeoutMs; }
        }
        public static class H2 {
            private long queryTimeoutMs = 0;
            public long getQueryTimeoutMs() { return queryTimeoutMs; }
            public void setQueryTimeoutMs(long queryTimeoutMs) { this.queryTimeoutMs = queryTimeoutMs; }
        }
        public static class Sqlite {
            private long busyTimeoutMs = 0;
            public long getBusyTimeoutMs() { return busyTimeoutMs; }
            public void setBusyTimeoutMs(long busyTimeoutMs) { this.busyTimeoutMs = busyTimeoutMs; }
        }
        public static class Mongodb {
            private long maxTimeMs = 0;
            private boolean enabled = true;
            public long getMaxTimeMs() { return maxTimeMs; }
            public void setMaxTimeMs(long maxTimeMs) { this.maxTimeMs = maxTimeMs; }
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
        }
        private Postgresql postgresql = new Postgresql();
        private Mysql mysql = new Mysql();
        private Oracle oracle = new Oracle();
        private SqlServer sqlServer = new SqlServer();
        private H2 h2 = new H2();
        private Sqlite sqlite = new Sqlite();
        private Mongodb mongodb = new Mongodb();
        
        public Postgresql getPostgresql() { return postgresql; }
        public Mysql getMysql() { return mysql; }
        public Oracle getOracle() { return oracle; }
        public SqlServer getSqlServer() { return sqlServer; }
        public H2 getH2() { return h2; }
        public Sqlite getSqlite() { return sqlite; }
        public Mongodb getMongodb() { return mongodb; }
        
        public void setPostgresql(Postgresql p) { this.postgresql = p; }
        public void setMysql(Mysql m) { this.mysql = m; }
        public void setOracle(Oracle o) { this.oracle = o; }
        public void setSqlServer(SqlServer s) { this.sqlServer = s; }
        public void setH2(H2 h) { this.h2 = h; }
        public void setSqlite(Sqlite s) { this.sqlite = s; }
        public void setMongodb(Mongodb m) { this.mongodb = m; }
    }
    private Vendor vendor = new Vendor();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Deprecated
    public long getThresholdMs() { return logThresholdMs; }

    @Deprecated
    public void setThresholdMs(long thresholdMs) { this.logThresholdMs = thresholdMs; }

    public long getLogThresholdMs() { return logThresholdMs; }
    public void setLogThresholdMs(long logThresholdMs) { this.logThresholdMs = logThresholdMs; }

    public long getCancelThresholdMs() { return cancelThresholdMs; }
    public void setCancelThresholdMs(long cancelThresholdMs) { this.cancelThresholdMs = cancelThresholdMs; }

    public boolean isCancelEnabled() { return cancelEnabled; }
    public void setCancelEnabled(boolean cancelEnabled) { this.cancelEnabled = cancelEnabled; }

    public boolean isOnlySlow() { return onlySlow; }
    public void setOnlySlow(boolean onlySlow) { this.onlySlow = onlySlow; }

    public boolean isLogParams() { return logParams; }
    public void setLogParams(boolean logParams) { this.logParams = logParams; }

    public double getSampleRate() { return sampleRate; }
    public void setSampleRate(double sampleRate) { this.sampleRate = sampleRate; }

    public String getLoggerName() { return loggerName; }
    public void setLoggerName(String loggerName) { this.loggerName = loggerName; }

    public String getExcludeSqlRegex() { return excludeSqlRegex; }
    public void setExcludeSqlRegex(String excludeSqlRegex) { this.excludeSqlRegex = excludeSqlRegex; }

    public LogFormat getLogFormat() { return logFormat; }
    public void setLogFormat(LogFormat logFormat) { this.logFormat = logFormat; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
}
