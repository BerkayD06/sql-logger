package com.berkayd06.query_logger.core;

import com.berkayd06.query_logger.config.QueryLoggerProperties;
import com.berkayd06.query_logger.logging.FastQueryLogger;
import com.berkayd06.query_logger.logging.QueryLogger;
import com.berkayd06.query_logger.util.QueryOrigin;
import com.berkayd06.query_logger.util.SqlFormatter;
import com.berkayd06.query_logger.vendor.VendorDialectHelper;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class QueryLoggingDataSource implements DataSource {
    
    private static final String[] EXECUTE_METHOD_PREFIXES = {
        "execute", "addBatch", "getResultSet", "executeQuery", 
        "executeUpdate", "executeLargeUpdate"
    };
    
    private final DataSource target;
    private final QueryLoggerProperties props;
    private final ScheduledExecutorService scheduler;
    private final QueryLogger queryLogger;
    private final Pattern excludePattern;
    private final boolean shouldLog;
    private final boolean shouldCancel;
    private final long logThresholdMs;
    private final long cancelThresholdMs;
    private final AtomicReference<VendorDialectHelper.DatabaseVendor> cachedVendor = new AtomicReference<>();

    public QueryLoggingDataSource(DataSource target, QueryLoggerProperties props) {
        this.target = Objects.requireNonNull(target, "Target DataSource cannot be null");
        this.props = Objects.requireNonNull(props, "QueryLoggerProperties cannot be null");
        this.queryLogger = new FastQueryLogger();
        this.excludePattern = compilePattern(props.getExcludeSqlRegex());
        this.shouldLog = props.isEnabled();
        this.shouldCancel = props.isCancelEnabled();
        this.logThresholdMs = props.getLogThresholdMs();
        this.cancelThresholdMs = props.getCancelThresholdMs() > 0 
            ? props.getCancelThresholdMs() 
            : props.getLogThresholdMs();
        
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "query-logger-cancel-scheduler");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        });
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!scheduler.isShutdown()) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }, "query-logger-shutdown"));
    }

    private static Pattern compilePattern(String regex) {
        if (regex == null || regex.trim().isEmpty()) {
            return null;
        }
        try {
            return Pattern.compile(regex.trim(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        } catch (Exception e) {
            System.err.println("WARN  Invalid regex pattern for excludeSqlRegex: " + regex);
            return null;
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return wrap(target.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return wrap(target.getConnection(username, password));
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return target.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        target.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        target.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return target.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return target.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        if (iface.isInstance(target)) {
            return iface.cast(target);
        }
        if (target instanceof javax.sql.DataSource) {
            try {
                return target.unwrap(iface);
            } catch (SQLException e) {
                throw e;
            }
        }
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return true;
        }
        if (iface.isInstance(target)) {
            return true;
        }
        if (target instanceof javax.sql.DataSource) {
            return target.isWrapperFor(iface);
        }
        return false;
    }

    public DataSource getTargetDataSource() {
        return target;
    }

    private Connection wrap(final Connection connection) {
        if (cachedVendor.get() == null && connection != null) {
            VendorDialectHelper.DatabaseVendor vendor = VendorDialectHelper.detectVendor(connection);
            cachedVendor.compareAndSet(null, vendor);
        }
        return (Connection) Proxy.newProxyInstance(
            connection.getClass().getClassLoader(),
            new Class[]{Connection.class},
            new ConnectionInvocationHandler(connection)
        );
    }

    private class ConnectionInvocationHandler implements InvocationHandler {
        private final Connection connection;

        ConnectionInvocationHandler(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (!needsInterception(methodName)) {
                return method.invoke(connection, args);
            }
            
            if ("createStatement".equals(methodName)) {
                Statement stmt = (Statement) method.invoke(connection, args);
                return wrapStatement(connection, stmt);
            }
            
            if ("prepareStatement".equals(methodName) || "prepareCall".equals(methodName)) {
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    String sql = (String) args[0];
                    VendorDialectHelper.DatabaseVendor vendor = cachedVendor.get();
                    String modifiedSql = VendorDialectHelper.maybeAddVendorHints(
                        sql, vendor, props);
                    if (modifiedSql != null && !modifiedSql.equals(sql)) {
                        args[0] = modifiedSql;
                    }
                }
                Statement stmt = (Statement) method.invoke(connection, args);
                return wrapStatement(connection, stmt);
            }
            
            return method.invoke(connection, args);
        }
        
        private boolean needsInterception(String methodName) {
            return "createStatement".equals(methodName) 
                || "prepareStatement".equals(methodName) 
                || "prepareCall".equals(methodName);
        }
    }

    private Statement wrapStatement(final Connection connection, final Statement statement) {
        final Map<Integer, Object> params = new ConcurrentHashMap<>(4);
        
        return (Statement) Proxy.newProxyInstance(
            statement.getClass().getClassLoader(),
            statement.getClass().getInterfaces(),
            new StatementInvocationHandler(connection, statement, params)
        );
    }

    private class StatementInvocationHandler implements InvocationHandler {
        private final Connection connection;
        private final Statement statement;
        private final Map<Integer, Object> params;

        StatementInvocationHandler(Connection connection, Statement statement, 
                                   Map<Integer, Object> params) {
            this.connection = connection;
            this.statement = statement;
            this.params = params;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.startsWith("set") && args != null && args.length >= 2
                && args[0] instanceof Integer) {
                params.put((Integer) args[0], args.length > 1 ? args[1] : null);
                return method.invoke(statement, args);
            }

            if (!isExecuteLike(methodName)) {
                return method.invoke(statement, args);
            }

            return handleExecuteOperation(method, args);
        }

        private Object handleExecuteOperation(Method method, Object[] args) throws Throwable {
            String rawSql = extractSqlArgOrToString(statement, args);
            if (rawSql == null) rawSql = "";
            boolean shouldLogThisQuery = shouldLog;
            if (shouldLogThisQuery) {
                if (excludePattern != null && excludePattern.matcher(rawSql).find()) {
                    shouldLogThisQuery = false;
                } else if (!sampleHit(props.getSampleRate())) {
                    shouldLogThisQuery = false;
                }
            }

            VendorDialectHelper.DatabaseVendor vendor = cachedVendor.get();
            if (vendor != null && vendor != VendorDialectHelper.DatabaseVendor.UNKNOWN) {
                VendorDialectHelper.maybeApplyVendorStatementTimeout(connection, vendor, props);
            }

            if (shouldCancel && cancelThresholdMs >= 1000) {
                try {
                    statement.setQueryTimeout((int) Math.max(1, (cancelThresholdMs + 999) / 1000));
                } catch (SQLException ignored) {}
            }

            long startNanos = System.nanoTime();
            ScheduledFuture<?> canceller = null;
            boolean canceled = false;

            if (shouldCancel && cancelThresholdMs > 0 && cancelThresholdMs < 1000 
                && !scheduler.isShutdown() && !scheduler.isTerminated()) {
                long cancelMs = cancelThresholdMs;
                try {
                    canceller = scheduler.schedule(() -> {
                        try {
                            statement.cancel();
                        } catch (SQLException e) {
                            queryLogger.logWarning("Failed to cancel long-running query: " + e.getMessage());
                        }
                    }, cancelMs, TimeUnit.MILLISECONDS);
                } catch (java.util.concurrent.RejectedExecutionException e) {
                }
            }

            try {
                Object result = method.invoke(statement, args);
                return result;
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getTargetException();
                canceled = isTimeoutOrCancel(cause);
                throw unwrapException(ite);
            } finally {
                long durationNanos = System.nanoTime() - startNanos;
                double durationMs = durationNanos / 1_000_000.0;
                if (canceller != null && !scheduler.isShutdown() && !scheduler.isTerminated()) {
                    try {
                        canceller.cancel(false);
                    } catch (java.util.concurrent.RejectedExecutionException e) {
                    } catch (Exception e) {
                    }
                }
                if (shouldLogThisQuery || canceled) {
                    logQuery(rawSql, durationMs, canceled);
                }
                cleanupOrigin();
            }
        }

        private void logQuery(String rawSql, double durationMs, boolean canceled) {
            if (!shouldLog) return;
            if (props.isOnlySlow() && durationMs < logThresholdMs) return;

            String formattedSql = formatSqlIfNeeded(rawSql);
            String origin = QueryOrigin.current();

            if (props.isLogParams() && props.getLogFormat() != QueryLoggerProperties.LogFormat.BOUND) {
                String paramsStr = formatParams(params);
                queryLogger.logQuery(props.getLoggerName(), origin, durationMs, canceled, formattedSql, paramsStr);
            } else {
                queryLogger.logQueryWithoutParams(props.getLoggerName(), origin, durationMs, canceled, formattedSql);
            }
        }

        private String formatSqlIfNeeded(String rawSql) {
            if (rawSql == null || rawSql.isEmpty()) {
                return "";
            }
            
            QueryLoggerProperties.LogFormat format = props.getLogFormat();
            if (format == QueryLoggerProperties.LogFormat.SQL) {
                return sanitize(rawSql);
            }
            
            if (format == QueryLoggerProperties.LogFormat.BOUND && !params.isEmpty()) {
                return SqlFormatter.formatQuery(rawSql, params, format);
            }
            
            if (format == QueryLoggerProperties.LogFormat.FORMATTED) {
                return SqlFormatter.formatQuery(rawSql, null, format);
            }
            
            return sanitize(rawSql);
        }

        private Throwable unwrapException(InvocationTargetException ite) throws SQLException {
            Throwable cause = ite.getTargetException();
            if (cause instanceof SQLException) {
                throw (SQLException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            return ite;
        }

        private void cleanupOrigin() {
            String current = QueryOrigin.current();
            if ("JPA".equals(current) || "MONGODB".equals(current)) {
                QueryOrigin.clear();
            }
        }
    }

    private static boolean isExecuteLike(String methodName) {
        for (String prefix : EXECUTE_METHOD_PREFIXES) {
            if (methodName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String extractSqlArgOrToString(Statement statement, Object[] args) {
        if (args != null && args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        try {
            String str = statement.toString();
            return str;
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean isTimeoutOrCancel(Throwable throwable) {
        if (throwable instanceof SQLTimeoutException) {
            return true;
        }
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        return lowerMessage.contains("cancel") || lowerMessage.contains("timeout");
    }

    private static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        int len = input.length();
        char[] result = new char[len];
        int writePos = 0;
        boolean lastWasSpace = false;
        
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (c == '\n' || c == '\r') {
                if (writePos == 0 || !lastWasSpace) {
                    result[writePos++] = ' ';
                    lastWasSpace = true;
                }
            } else if (Character.isWhitespace(c)) {
                if (writePos == 0 || !lastWasSpace) {
                    result[writePos++] = ' ';
                    lastWasSpace = true;
                }
            } else {
                result[writePos++] = c;
                lastWasSpace = false;
            }
        }
        
        while (writePos > 0 && result[writePos - 1] == ' ') {
            writePos--;
        }
        
        return new String(result, 0, writePos);
    }

    private static String formatParams(Map<Integer, Object> params) {
        if (params == null || params.isEmpty()) {
            return "{}";
        }
        
        List<Integer> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        
        int size = keys.size();
        StringBuilder sb = new StringBuilder(size * 20);
        sb.append('{');
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Integer key = keys.get(i);
            Object value = params.get(key);
            sb.append(key).append(':');
            if (value != null) {
                sb.append(value.toString());
            } else {
                sb.append("null");
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static boolean sampleHit(double rate) {
        if (rate >= 1.0) {
            return true;
        }
        if (rate <= 0.0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < rate;
    }
}
