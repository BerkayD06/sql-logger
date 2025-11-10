package com.berkayd06.query_logger.util;

public final class QueryOrigin {
    private static final ThreadLocal<String> ORIGIN = new ThreadLocal<String>();

    private QueryOrigin() {}

    public static void markJpa() { ORIGIN.set("JPA"); }
    public static void markMongo() { ORIGIN.set("MONGODB"); }
    public static void clear()   { ORIGIN.remove(); }
    public static String current() {
        String s = ORIGIN.get();
        return s == null ? "NATIVE" : s;
    }
}
