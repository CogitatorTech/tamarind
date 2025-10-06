package io.github.cogitatortech.tamarind.cache;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Utility class to normalize SQL queries for better cache key generation. */
public class SqlNormalizer {

  private static final Logger LOGGER = LogManager.getLogger(SqlNormalizer.class);

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
  private static final Pattern COMMENT_PATTERN =
      Pattern.compile("--[^\n]*|/\\*.*?\\*/", Pattern.DOTALL);

  /**
   * Normalize SQL query to improve cache hit rates.
   *
   * <p>Normalization includes: - Converting to lowercase - Removing extra whitespace - Removing
   * comments - Trimming leading/trailing whitespace
   *
   * @param sql The raw SQL query
   * @return Normalized SQL string
   */
  public static String normalize(String sql) {
    if (sql == null || sql.isEmpty()) {
      return ""; // Return empty string instead of null to prevent NPE
    }

    try {
      // Remove comments
      String normalized = COMMENT_PATTERN.matcher(sql).replaceAll("");

      // Convert to lowercase for case-insensitive matching
      normalized = normalized.toLowerCase(Locale.ROOT);

      // Replace multiple whitespaces with single space
      normalized = WHITESPACE_PATTERN.matcher(normalized).replaceAll(" ");

      // Trim leading/trailing whitespace
      normalized = normalized.trim();

      return normalized;
    } catch (Exception e) {
      LOGGER.warn("Failed to normalize SQL, using original: {}", e.getMessage());
      return sql;
    }
  }

  /**
   * Generate a SHA-256 hash of the normalized SQL for use as cache key.
   *
   * @param sql The SQL query
   * @return Hex string of SHA-256 hash
   */
  public static String generateCacheKey(String sql) {
    String normalized = normalize(sql);
    return sha256Hex(normalized);
  }

  private static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes());
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }
}
