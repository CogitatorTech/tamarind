package io.github.cogitatortech.tamarind.engine;

import io.github.cogitatortech.tamarind.cache.SqlNormalizer;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Query optimization layer that provides query validation, rewriting, and basic cost estimation.
 */
@ApplicationScoped
public class QueryOptimizer {

  private static final Logger LOGGER = LogManager.getLogger(QueryOptimizer.class);

  // Query validation patterns
  private static final Pattern SELECT_PATTERN =
      Pattern.compile("^\\s*SELECT", Pattern.CASE_INSENSITIVE);
  private static final Pattern DANGEROUS_PATTERN =
      Pattern.compile("(DROP|DELETE|TRUNCATE|ALTER)\\s+", Pattern.CASE_INSENSITIVE);
  private static final Pattern MULTIPLE_STATEMENTS =
      Pattern.compile(";\\s*(?=\\S)", Pattern.CASE_INSENSITIVE);

  // Query rewriting patterns
  private static final Pattern SELECT_STAR =
      Pattern.compile("SELECT\\s+\\*\\s+FROM", Pattern.CASE_INSENSITIVE);
  private static final Pattern LIMIT_PATTERN =
      Pattern.compile("\\s+LIMIT\\s+\\d+", Pattern.CASE_INSENSITIVE);

  /**
   * Validate a SQL query for security and syntax issues.
   *
   * @param sql the SQL query to validate
   * @return validation result with any issues found
   */
  public ValidationResult validate(String sql) {
    List<String> warnings = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    if (sql == null || sql.trim().isEmpty()) {
      errors.add("Query cannot be null or empty");
      return new ValidationResult(false, errors, warnings);
    }

    // Check for dangerous operations
    if (DANGEROUS_PATTERN.matcher(sql).find()) {
      errors.add("Dangerous SQL operation detected (DROP, DELETE, TRUNCATE, ALTER)");
      return new ValidationResult(false, errors, warnings);
    }

    // Check for multiple statements (SQL injection risk)
    Matcher multipleStmts = MULTIPLE_STATEMENTS.matcher(sql);
    int count = 0;
    while (multipleStmts.find()) {
      count++;
    }
    if (count > 0) {
      warnings.add("Multiple SQL statements detected - only the first will be executed");
    }

    // Check for SELECT *
    if (SELECT_STAR.matcher(sql).find()) {
      warnings.add(
          "SELECT * detected - consider specifying explicit columns for better performance");
    }

    // Check for missing LIMIT clause on SELECT
    if (SELECT_PATTERN.matcher(sql).find() && !LIMIT_PATTERN.matcher(sql).find()) {
      warnings.add("No LIMIT clause found - query may return large result set");
    }

    return new ValidationResult(true, errors, warnings);
  }

  /**
   * Optimize a SQL query by applying rewrites and normalization.
   *
   * @param sql the SQL query to optimize
   * @param options optimization options
   * @return optimized query result
   */
  public OptimizationResult optimize(String sql, OptimizationOptions options) {
    String optimized = sql;
    List<String> appliedOptimizations = new ArrayList<>();

    // Normalize SQL (remove comments, normalize whitespace)
    String normalized = SqlNormalizer.normalize(sql);
    if (!normalized.equals(sql)) {
      optimized = normalized;
      appliedOptimizations.add("SQL normalization");
    }

    // Add default LIMIT if requested and missing
    if (options.addDefaultLimit
        && SELECT_PATTERN.matcher(optimized).find()
        && !LIMIT_PATTERN.matcher(optimized).find()) {
      // Validate limit value to prevent SQL injection
      if (options.defaultLimitValue <= 0 || options.defaultLimitValue > 1_000_000) {
        throw new IllegalArgumentException(
            "Invalid default limit value: " + options.defaultLimitValue);
      }
      optimized = optimized + " LIMIT " + options.defaultLimitValue;
      appliedOptimizations.add("Added default LIMIT " + options.defaultLimitValue);
    }

    // Estimate query cost (simple heuristic)
    QueryCost estimatedCost = estimateCost(optimized);

    LOGGER.debug("Applied {} optimizations to query", appliedOptimizations.size());

    return new OptimizationResult(optimized, appliedOptimizations, estimatedCost);
  }

  /**
   * Estimate the cost of executing a query (simple heuristic-based).
   *
   * @param sql the SQL query
   * @return estimated cost
   */
  private QueryCost estimateCost(String sql) {
    int cost = 0;
    String normalized = sql.toLowerCase();

    // Base cost
    cost += 10;

    // JOIN increases cost
    int joinCount = countOccurrences(normalized, "join");
    cost += joinCount * 50;

    // Subqueries increase cost
    int subqueryCount = countOccurrences(normalized, "select") - 1;
    cost += Math.max(0, subqueryCount) * 30;

    // GROUP BY increases cost
    if (normalized.contains("group by")) {
      cost += 20;
    }

    // ORDER BY increases cost
    if (normalized.contains("order by")) {
      cost += 15;
    }

    // DISTINCT increases cost
    if (normalized.contains("distinct")) {
      cost += 25;
    }

    // Determine complexity level
    QueryCost.Complexity complexity;
    if (cost < 50) {
      complexity = QueryCost.Complexity.SIMPLE;
    } else if (cost < 150) {
      complexity = QueryCost.Complexity.MODERATE;
    } else if (cost < 300) {
      complexity = QueryCost.Complexity.COMPLEX;
    } else {
      complexity = QueryCost.Complexity.VERY_COMPLEX;
    }

    return new QueryCost(cost, complexity);
  }

  private int countOccurrences(String text, String substring) {
    int count = 0;
    int index = 0;
    while ((index = text.indexOf(substring, index)) != -1) {
      count++;
      index += substring.length();
    }
    return count;
  }

  /** Optimization options for query rewriting. */
  public static class OptimizationOptions {
    public boolean addDefaultLimit = false;
    public int defaultLimitValue = 1000;
    public boolean normalizeWhitespace = true;

    public static OptimizationOptions defaults() {
      return new OptimizationOptions();
    }

    public static OptimizationOptions withDefaultLimit(int limit) {
      OptimizationOptions opts = new OptimizationOptions();
      opts.addDefaultLimit = true;
      opts.defaultLimitValue = limit;
      return opts;
    }
  }

  /** Result of query validation. */
  public static class ValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
      this.valid = valid;
      this.errors = errors;
      this.warnings = warnings;
    }

    public boolean isValid() {
      return valid;
    }

    public List<String> getErrors() {
      return errors;
    }

    public List<String> getWarnings() {
      return warnings;
    }

    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }
  }

  /** Result of query optimization. */
  public static class OptimizationResult {
    private final String optimizedSql;
    private final List<String> appliedOptimizations;
    private final QueryCost estimatedCost;

    public OptimizationResult(
        String optimizedSql, List<String> appliedOptimizations, QueryCost estimatedCost) {
      this.optimizedSql = optimizedSql;
      this.appliedOptimizations = appliedOptimizations;
      this.estimatedCost = estimatedCost;
    }

    public String getOptimizedSql() {
      return optimizedSql;
    }

    public List<String> getAppliedOptimizations() {
      return appliedOptimizations;
    }

    public QueryCost getEstimatedCost() {
      return estimatedCost;
    }
  }

  /** Estimated cost of a query. */
  public static class QueryCost {
    private final int cost;
    private final Complexity complexity;

    public QueryCost(int cost, Complexity complexity) {
      this.cost = cost;
      this.complexity = complexity;
    }

    public int getCost() {
      return cost;
    }

    public Complexity getComplexity() {
      return complexity;
    }

    public enum Complexity {
      SIMPLE,
      MODERATE,
      COMPLEX,
      VERY_COMPLEX
    }
  }
}
