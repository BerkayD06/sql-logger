package com.berkayd06.query_logger.compat;

public final class DataSourceCompat {
    
    private static final boolean IS_JAKARTA_AVAILABLE = checkJakartaAvailable();
    
    private static boolean checkJakartaAvailable() {
        try {
            Class.forName("jakarta.sql.DataSource");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public static boolean isJakartaAvailable() {
        return IS_JAKARTA_AVAILABLE;
    }
    
    public static Class<?> getDataSourceClass() {
        try {
            if (IS_JAKARTA_AVAILABLE) {
                return Class.forName("jakarta.sql.DataSource");
            } else {
                return Class.forName("javax.sql.DataSource");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("DataSource class not found", e);
        }
    }
}
