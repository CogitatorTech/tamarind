package io.github.cogitatortech.tamarind.rest.v1;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Utility for sanitizing query result rows for JSON responses. */
public final class RowSanitizer {
  private RowSanitizer() {}

  public static final int MAX_FIELD_STRING_LENGTH = 1000;
  public static final int MAX_ARRAY_ELEMENTS = 64;

  public static Sanitized sanitize(List<Map<String, Object>> rows) {
    boolean[] truncatedFlag = new boolean[] {false};
    List<Map<String, Object>> sanitized =
        rows.stream().map(r -> sanitizeRow(r, truncatedFlag)).toList();
    return new Sanitized(sanitized, truncatedFlag[0]);
  }

  private static Map<String, Object> sanitizeRow(Map<String, Object> row, boolean[] truncatedFlag) {
    Map<String, Object> out = new HashMap<>();
    for (Map.Entry<String, Object> e : row.entrySet()) {
      out.put(e.getKey(), sanitizeValue(e.getValue(), truncatedFlag));
    }
    return out;
  }

  private static Object sanitizeValue(Object value, boolean[] truncatedFlag) {
    if (value == null) return null;
    if (value instanceof Double d) {
      if (d.isNaN() || d.isInfinite()) return String.valueOf(d);
      return d;
    }
    if (value instanceof Float f) {
      if (f.isNaN() || f.isInfinite()) return String.valueOf(f);
      return f;
    }
    if (value instanceof CharSequence cs) {
      String s = cs.toString();
      if (s.length() > MAX_FIELD_STRING_LENGTH) {
        truncatedFlag[0] = true;
        return s.substring(0, MAX_FIELD_STRING_LENGTH) + "…";
      }
      return s;
    }
    if (value.getClass().isArray()) {
      int len = Array.getLength(value);
      List<Object> list = new java.util.ArrayList<>(Math.min(len, MAX_ARRAY_ELEMENTS));
      int limit = Math.min(len, MAX_ARRAY_ELEMENTS);
      for (int i = 0; i < limit; i++) {
        list.add(sanitizeValue(Array.get(value, i), truncatedFlag));
      }
      if (len > MAX_ARRAY_ELEMENTS) truncatedFlag[0] = true;
      return list;
    }
    if (value instanceof List<?> l) {
      int len = l.size();
      List<Object> out = new java.util.ArrayList<>(Math.min(len, MAX_ARRAY_ELEMENTS));
      int limit = Math.min(len, MAX_ARRAY_ELEMENTS);
      for (int i = 0; i < limit; i++) {
        out.add(sanitizeValue(l.get(i), truncatedFlag));
      }
      if (len > MAX_ARRAY_ELEMENTS) truncatedFlag[0] = true;
      return out;
    }
    if (value instanceof Map<?, ?> m) {
      Map<String, Object> out = new HashMap<>();
      for (Map.Entry<?, ?> e : m.entrySet()) {
        Object k = e.getKey();
        if (k != null) out.put(String.valueOf(k), sanitizeValue(e.getValue(), truncatedFlag));
      }
      return out;
    }
    if (value instanceof BigDecimal
        || value instanceof BigInteger
        || value instanceof Integer
        || value instanceof Long
        || value instanceof Short) {
      return value;
    }
    String s = String.valueOf(value);
    if (s.length() > MAX_FIELD_STRING_LENGTH) {
      truncatedFlag[0] = true;
      return s.substring(0, MAX_FIELD_STRING_LENGTH) + "…";
    }
    return s;
  }

  public record Sanitized(List<Map<String, Object>> rows, boolean truncated) {}
}
