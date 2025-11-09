package io.github.cogitatortech.tamarind.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Utility class to normalize SQL queries for better cache key generation. */
public class SqlNormalizer {

  private static final Logger LOGGER = LogManager.getLogger(SqlNormalizer.class);

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
  private static final Pattern COMMENT_PATTERN =
      Pattern.compile("--[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);

  /**
   * Normalize SQL query to improve cache hit rates.
   *
   * <p>Normalization includes: - Removing comments (handles nested block comments) - Converting to
   * lowercase (but preserving double-quoted identifiers) - Replacing string and numeric literals
   * with placeholders - Normalizing spacing around common operators - Removing extra whitespace
   *
   * @param sql The raw SQL query
   * @return Normalized SQL string
   */
  public static String normalize(String sql) {
    if (sql == null || sql.isEmpty()) {
      return ""; // Return empty string instead of null to prevent NPE
    }

    try {
      // First remove comments while respecting quoted strings and quoted identifiers
      String withoutComments = removeCommentsRespectingQuotes(sql);

      StringBuilder out = new StringBuilder(withoutComments.length());

      boolean inSingleQuote = false;
      boolean inDoubleQuote = false;

      for (int i = 0; i < withoutComments.length(); i++) {
        char c = withoutComments.charAt(i);
        if (c == '\'' && !inDoubleQuote) {
          // toggle single-quote, but handle doubled-single-quote escape
          if (inSingleQuote) {
            // lookahead for escaped quote ('' -> stays in single-quoted literal)
            if (i + 1 < withoutComments.length() && withoutComments.charAt(i + 1) == '\'') {
              // consume one of the two quotes and keep in single quote
              out.append("''");
              i++; // skip the second quote as we've consumed it
              continue;
            }
            inSingleQuote = false;
            out.append(c);
            continue;
          } else {
            inSingleQuote = true;
            out.append(c);
            continue;
          }
        }

        if (c == '"' && !inSingleQuote) {
          inDoubleQuote = !inDoubleQuote;
          out.append(c);
          continue;
        }

        out.append(c);
      }

      String processed = out.toString();

      // Replace all single-quoted string literals with a placeholder '?' (preserve double-quoted
      // identifiers)
      processed = processed.replaceAll("'([^']|'')*'", "?");

      // Replace numeric literals with placeholder '?'
      processed = processed.replaceAll("\\b\\d+(?:\\.\\d+)?\\b", "?");

      // Convert to lowercase while preserving content inside double quotes
      processed = lowercaseOutsideDoubleQuotes(processed);

      // Normalize spacing around common operators
      processed = processed.replaceAll("\\s*(=|<>|!=|<=|>=|<|>)\\s*", " $1 ");

      // Normalize comma spacing
      processed = processed.replaceAll("\\s*,\\s*", ", ");

      // Collapse multiple whitespace characters into a single space
      processed = WHITESPACE_PATTERN.matcher(processed).replaceAll(" ");

      // Trim
      processed = processed.trim();

      return processed;
    } catch (Exception e) {
      LOGGER.warn("Failed to normalize SQL, using original: {}", e.getMessage());
      return sql;
    }
  }

  // Remove comments while respecting quoted strings and double-quoted identifiers; supports nested
  // /* */
  private static String removeCommentsRespectingQuotes(String sql) {
    StringBuilder sb = new StringBuilder(sql.length());
    int len = sql.length();

    boolean inSingle = false;
    boolean inDouble = false;

    for (int i = 0; i < len; ) {
      char c = sql.charAt(i);

      // handle start of single-line comment -- when not inside quotes
      if (!inSingle && !inDouble && c == '-' && i + 1 < len && sql.charAt(i + 1) == '-') {
        // skip until end of line
        i += 2;
        while (i < len && sql.charAt(i) != '\n') {
          i++;
        }
        // skip the newline as well
        if (i < len && sql.charAt(i) == '\n') {
          i++;
        }
        continue;
      }

      // handle block comments /* ... */ with nesting when not inside quotes
      if (!inSingle && !inDouble && c == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
        i += 2;
        int depth = 1;
        while (i < len && depth > 0) {
          if (i + 1 < len && sql.charAt(i) == '/' && sql.charAt(i + 1) == '*') {
            depth++;
            i += 2;
            continue;
          }
          if (i + 1 < len && sql.charAt(i) == '*' && sql.charAt(i + 1) == '/') {
            depth--;
            i += 2;
            continue;
          }
          // don't treat comment markers inside single/double quotes as comment delimiters
          if (sql.charAt(i) == '\'') {
            // skip single-quoted literals inside comments correctly
            i++;
            while (i < len) {
              if (sql.charAt(i) == '\'') {
                if (i + 1 < len && sql.charAt(i + 1) == '\'') {
                  i += 2; // escaped quote
                } else {
                  i++;
                  break;
                }
              } else {
                i++;
              }
            }
            continue;
          }
          if (sql.charAt(i) == '"') {
            // skip double-quoted identifiers inside comments
            i++;
            while (i < len) {
              if (sql.charAt(i) == '"') {
                i++;
                break;
              } else {
                i++;
              }
            }
            continue;
          }
          i++;
        }
        continue; // skip adding anything for the comment
      }

      // toggle quote flags when encountering quote characters outside the other quote type
      if (c == '\'' && !inDouble) {
        inSingle = !inSingle;
        sb.append(c);
        i++;
        continue;
      }
      if (c == '"' && !inSingle) {
        inDouble = !inDouble;
        sb.append(c);
        i++;
        continue;
      }

      // normal character
      sb.append(c);
      i++;
    }

    return sb.toString();
  }

  // Lowercase characters outside double-quoted identifiers and leave quoted identifiers as-is
  private static String lowercaseOutsideDoubleQuotes(String s) {
    StringBuilder out = new StringBuilder(s.length());
    boolean inDouble = false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '"') {
        inDouble = !inDouble;
        out.append(c);
        continue;
      }
      if (inDouble) {
        out.append(c); // preserve case inside double quotes
      } else {
        out.append(Character.toLowerCase(c));
      }
    }
    return out.toString();
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

  /**
   * Normalize SQL for result caching: preserves string and numeric literals to ensure queries with
   * different constant predicates do not collide.
   *
   * @param sql raw SQL
   * @return normalized SQL retaining literal semantics
   */
  public static String normalizeForResultCache(String sql) {
    if (sql == null || sql.isEmpty()) {
      return "";
    }
    try {
      // Reuse comment removal + quote handling from normalize path but without literal replacement
      String withoutComments = removeCommentsRespectingQuotes(sql);
      // Lowercase outside double quotes and collapse whitespace similar to normalize but keep
      // literals
      String processed = lowercaseOutsideDoubleQuotes(withoutComments);

      // Normalize spacing around operators
      processed = processed.replaceAll("\\s*(=|<>|!=|<=|>=|<|>)\\s*", " $1 ");
      // Normalize comma spacing
      processed = processed.replaceAll("\\s*,\\s*", ", ");
      // Collapse whitespace
      processed = WHITESPACE_PATTERN.matcher(processed).replaceAll(" ");
      return processed.trim();
    } catch (Exception e) {
      LOGGER.warn("Failed to normalize (result cache) SQL, using original: {}", e.getMessage());
      return sql;
    }
  }

  /**
   * Generate cache key for result caching using normalization that preserves literals.
   *
   * @param sql raw SQL
   * @return hash key
   */
  public static String generateResultCacheKey(String sql) {
    return sha256Hex(normalizeForResultCache(sql));
  }

  private static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
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
