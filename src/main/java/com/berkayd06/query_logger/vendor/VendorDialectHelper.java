package com.berkayd06.query_logger.vendor;

import com.berkayd06.query_logger.config.QueryLoggerProperties;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public final class VendorDialectHelper {
    private VendorDialectHelper(){}
    public enum DatabaseVendor {
        POSTGRESQL,
        MYSQL,
        ORACLE,
        SQL_SERVER,
        H2,
        SQLITE,
        MONGODB,
        UNKNOWN
    }
    public static DatabaseVendor detectVendor(Connection connection) {
        if (connection == null) return DatabaseVendor.UNKNOWN;
        
        try {
            DatabaseMetaData md = connection.getMetaData();
            String product = md.getDatabaseProductName();
            if (product == null) return DatabaseVendor.UNKNOWN;
            
            String productLower = product.toLowerCase();
            
            if (productLower.contains("postgresql") || productLower.contains("postgres")) {
                return DatabaseVendor.POSTGRESQL;
            } else if (productLower.contains("mysql")) {
                return DatabaseVendor.MYSQL;
            } else if (productLower.contains("oracle")) {
                return DatabaseVendor.ORACLE;
            } else if (productLower.contains("microsoft sql server") || productLower.contains("sql server")) {
                return DatabaseVendor.SQL_SERVER;
            } else if (productLower.contains("h2")) {
                return DatabaseVendor.H2;
            } else if (productLower.contains("sqlite")) {
                return DatabaseVendor.SQLITE;
            } else {
                return DatabaseVendor.UNKNOWN;
            }
        } catch (SQLException ignore) {
            return DatabaseVendor.UNKNOWN;
        }
    }

    public static String maybeAddVendorHints(String originalSql,
                                             Connection connection,
                                             QueryLoggerProperties props) {
        if (originalSql == null) return null;
        String sql = originalSql;
        DatabaseVendor vendor = detectVendor(connection);

        try {
            switch (vendor) {
                case MYSQL:
                    long mysqlMs = props.getVendor().getMysql().getMaxExecutionTimeMs();
                    if (mysqlMs > 0) {
                        sql = "/*+ MAX_EXECUTION_TIME(" + mysqlMs + ") */ " + sql;
                    }
                    break;
                    
                case POSTGRESQL:
                    break;
                    
                case ORACLE:
                    break;
                    
                case SQL_SERVER:
                    break;
                    
                case H2:
                case SQLITE:
                    break;
                    
                default:
                    break;
            }
        } catch (Exception ignore) {}

        return sql;
    }
    public static void maybeApplyVendorStatementTimeout(Connection connection, QueryLoggerProperties props) {
        if (connection == null) return;
        
        DatabaseVendor vendor = detectVendor(connection);
        java.sql.Statement st = null;
        
        try {
            switch (vendor) {
                case POSTGRESQL:
                    long pgMs = props.getVendor().getPostgresql().getStatementTimeoutMs();
                    if (pgMs > 0) {
                        st = connection.createStatement();
                        st.execute("SET LOCAL statement_timeout = " + pgMs);
                    }
                    break;
                    
                case ORACLE:
                    long oracleMs = props.getVendor().getOracle().getStatementTimeoutMs();
                    if (oracleMs > 0) {
                        st = connection.createStatement();
                        st.execute("ALTER SESSION SET MAX_IDLE_TIME = " + (oracleMs / 1000));
                    }
                    break;
                    
                case SQL_SERVER:
                    long sqlServerMs = props.getVendor().getSqlServer().getQueryTimeoutMs();
                    if (sqlServerMs > 0) {
                    }
                    break;
                    
                case H2:
                    long h2Ms = props.getVendor().getH2().getQueryTimeoutMs();
                    if (h2Ms > 0) {
                    }
                    break;
                    
                case SQLITE:
                    long sqliteMs = props.getVendor().getSqlite().getBusyTimeoutMs();
                    if (sqliteMs > 0) {
                        st = connection.createStatement();
                        st.execute("PRAGMA busy_timeout = " + sqliteMs);
                    }
                    break;
                    
                default:
                    break;
            }
        } catch (Exception ignore) {
        } finally {
            if (st != null) {
                try { 
                    st.close(); 
                } catch (Exception ignore) {}
            }
        }
    }

    @Deprecated
    public static void maybeApplyPgStatementTimeout(Connection connection, QueryLoggerProperties props) {
        maybeApplyVendorStatementTimeout(connection, props);
    }
}
