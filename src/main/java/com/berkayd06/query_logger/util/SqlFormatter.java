package com.berkayd06.query_logger.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlFormatter {
    private SqlFormatter() {}

    private static final Pattern SQL_KEYWORDS = Pattern.compile(
        "\\b(SELECT|FROM|WHERE|JOIN|INNER|LEFT|RIGHT|OUTER|ON|GROUP|BY|ORDER|HAVING|" +
        "INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|ALTER|DROP|INDEX|TABLE|DATABASE|" +
        "AND|OR|NOT|IN|LIKE|BETWEEN|IS|NULL|AS|ASC|DESC|LIMIT|OFFSET|UNION|ALL|" +
        "COUNT|SUM|AVG|MAX|MIN|DISTINCT|CASE|WHEN|THEN|ELSE|END)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern COMMA_WHITESPACE = Pattern.compile("\\s*,\\s*");
    private static final Pattern PAREN_OPEN = Pattern.compile("\\s*\\(\\s*");
    private static final Pattern PAREN_CLOSE = Pattern.compile("\\s*\\)\\s*");
    private static final Pattern EQUALS_WHITESPACE = Pattern.compile("\\s*=\\s*");
    private static final Pattern SEMICOLON_WHITESPACE = Pattern.compile("\\s*;\\s*");
    
    private static final Pattern SELECT_PATTERN = Pattern.compile("\\s+(SELECT)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM_PATTERN = Pattern.compile("\\s+(FROM)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHERE_PATTERN = Pattern.compile("\\s+(WHERE)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_PATTERN = Pattern.compile("\\s+(JOIN|INNER JOIN|LEFT JOIN|RIGHT JOIN|OUTER JOIN)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_BY_PATTERN = Pattern.compile("\\s+(GROUP BY)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile("\\s+(ORDER BY)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern HAVING_PATTERN = Pattern.compile("\\s+(HAVING)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNION_PATTERN = Pattern.compile("\\s+(UNION|UNION ALL)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_PATTERN = Pattern.compile("\\s+(INSERT INTO)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_PATTERN = Pattern.compile("\\s+(UPDATE)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_PATTERN = Pattern.compile("\\s+(DELETE FROM)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern AND_OR_PATTERN = Pattern.compile("\\s+(AND|OR)\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTI_NEWLINE = Pattern.compile("\n+");

    public static String format(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        String trimmed = sql.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        int len = trimmed.length();
        StringBuilder sb = new StringBuilder(len + 50);
        int pos = 0;
        
        while (pos < len) {
            char c = trimmed.charAt(pos);
            
            if (Character.isLetter(c)) {
                int wordStart = pos;
                int wordEnd = pos;
                while (wordEnd < len && Character.isLetterOrDigit(trimmed.charAt(wordEnd))) {
                    wordEnd++;
                }
                
                String word = trimmed.substring(wordStart, wordEnd);
                String upperWord = word.toUpperCase();
                if (isKeyword(upperWord)) {
                    sb.append(upperWord);
                } else {
                    sb.append(word);
                }
                
                pos = wordEnd;
            } else {
                sb.append(c);
                pos++;
            }
        }
        
        String formatted = sb.toString();

        formatted = MULTI_WHITESPACE.matcher(formatted).replaceAll(" ");
        formatted = COMMA_WHITESPACE.matcher(formatted).replaceAll(", ");
        formatted = PAREN_OPEN.matcher(formatted).replaceAll(" (");
        formatted = PAREN_CLOSE.matcher(formatted).replaceAll(") ");
        formatted = EQUALS_WHITESPACE.matcher(formatted).replaceAll(" = ");
        formatted = SEMICOLON_WHITESPACE.matcher(formatted).replaceAll(";");

        formatted = SELECT_PATTERN.matcher(formatted).replaceAll("\n$1 ");
        formatted = FROM_PATTERN.matcher(formatted).replaceAll("\n$1 ");
        formatted = WHERE_PATTERN.matcher(formatted).replaceAll("\n$1 ");
        formatted = JOIN_PATTERN.matcher(formatted).replaceAll("\n$1 ");
        formatted = GROUP_BY_PATTERN.matcher(formatted).replaceAll("\n$1 ");
        formatted = ORDER_BY_PATTERN.matcher(formatted).replaceAll("\n$1 ");
        formatted = HAVING_PATTERN.matcher(formatted).replaceAll("\n$1 ");
        formatted = UNION_PATTERN.matcher(formatted).replaceAll("\n$1 ");
        formatted = INSERT_PATTERN.matcher(formatted).replaceAll("\n$1 ");
        formatted = UPDATE_PATTERN.matcher(formatted).replaceAll("\n$1 ");
        formatted = DELETE_PATTERN.matcher(formatted).replaceAll("\n$1 ");
        formatted = AND_OR_PATTERN.matcher(formatted).replaceAll("\n  $1 ");
        
        formatted = MULTI_NEWLINE.matcher(formatted).replaceAll("\n");
        
        return formatted.trim();
    }

    private static boolean isKeyword(String word) {
        switch (word) {
            case "SELECT": case "FROM": case "WHERE": case "JOIN":
            case "INNER": case "LEFT": case "RIGHT": case "OUTER":
            case "ON": case "GROUP": case "BY": case "ORDER":
            case "HAVING": case "INSERT": case "INTO": case "VALUES":
            case "UPDATE": case "SET": case "DELETE": case "CREATE":
            case "ALTER": case "DROP": case "INDEX": case "TABLE":
            case "DATABASE": case "AND": case "OR": case "NOT":
            case "IN": case "LIKE": case "BETWEEN": case "IS":
            case "NULL": case "AS": case "ASC": case "DESC":
            case "LIMIT": case "OFFSET": case "UNION": case "ALL":
            case "COUNT": case "SUM": case "AVG": case "MAX":
            case "MIN": case "DISTINCT": case "CASE": case "WHEN":
            case "THEN": case "ELSE": case "END":
                return true;
            default:
                return false;
        }
    }

    public static String bindParameters(String sql, Map<Integer, Object> params) {
        if (sql == null || params == null || params.isEmpty()) {
            return sql;
        }

        List<Integer> indices = new ArrayList<>(params.keySet());
        Collections.sort(indices);

        if (indices.isEmpty()) {
            return sql;
        }

        String result = sql;
        for (int i = indices.size() - 1; i >= 0; i--) {
            Integer paramIdx = indices.get(i);
            Object paramValue = params.get(paramIdx);
            String valueStr = formatParameterValue(paramValue);
            
            int questionMarkPos = findNthQuestionMark(result, paramIdx, 0);
            if (questionMarkPos >= 0 && questionMarkPos < result.length()) {
                StringBuilder sb = new StringBuilder(result.length() + valueStr.length() - 1);
                sb.append(result, 0, questionMarkPos);
                sb.append(valueStr);
                sb.append(result, questionMarkPos + 1, result.length());
                result = sb.toString();
            }
        }

        return result;
    }

    private static int findNthQuestionMark(String sql, int n, int startPos) {
        int count = 0;
        int length = sql.length();
        for (int i = startPos; i < length; i++) {
            if (sql.charAt(i) == '?') {
                count++;
                if (count == n) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String formatParameterValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof String) {
            String str = (String) value;
            StringBuilder sb = new StringBuilder(str.length() + 2);
            sb.append('\'');
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '\'') {
                    sb.append("''");
                } else if (c == '\\') {
                    sb.append("\\\\");
                } else {
                    sb.append(c);
                }
            }
            sb.append('\'');
            return sb.toString();
        }
        
        if (value instanceof Number) {
            return value.toString();
        }
        
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        
        if (value instanceof java.util.Date) {
            java.sql.Timestamp ts = new java.sql.Timestamp(((java.util.Date) value).getTime());
            return "'" + ts.toString() + "'";
        }
        
        String str = value.toString();
        StringBuilder sb = new StringBuilder(str.length() + 2);
        sb.append('\'');
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\'') {
                sb.append("''");
            } else {
                sb.append(c);
            }
        }
        sb.append('\'');
        return sb.toString();
    }

    public static String formatQuery(String sql, Map<Integer, Object> params, 
                                     com.berkayd06.query_logger.config.QueryLoggerProperties.LogFormat format) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }

        switch (format) {
            case SQL:
                return sanitize(sql);
            case FORMATTED:
                return format(sql);
            case BOUND:
                if (params != null && !params.isEmpty()) {
                    return bindParameters(format(sql), params);
                }
                return format(sql);
            default:
                return sanitize(sql);
        }
    }

    private static String sanitize(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }
        
        int len = sql.length();
        char[] result = new char[len];
        int writePos = 0;
        boolean lastWasSpace = false;
        
        for (int i = 0; i < len; i++) {
            char c = sql.charAt(i);
            if (c == '\n' || c == '\r') {
                if (!lastWasSpace) {
                    result[writePos++] = ' ';
                    lastWasSpace = true;
                }
            } else if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    result[writePos++] = ' ';
                    lastWasSpace = true;
                }
            } else {
                result[writePos++] = c;
                lastWasSpace = false;
            }
        }
        
        int start = 0;
        int end = writePos;
        
        while (start < end && result[start] == ' ') {
            start++;
        }
        while (end > start && result[end - 1] == ' ') {
            end--;
        }
        
        return new String(result, start, end - start);
    }
}
