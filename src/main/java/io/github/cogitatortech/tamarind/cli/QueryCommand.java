package io.github.cogitatortech.tamarind.cli;

import io.github.cogitatortech.tamarind.config.EngineConfig;
import io.github.cogitatortech.tamarind.engine.DuckDBConnectionPool;
import io.github.cogitatortech.tamarind.engine.DuckDBEngine;
import io.github.cogitatortech.tamarind.engine.QueryResult;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Command for executing SQL queries against DuckDB. */
@Command(
    name = "query",
    description = "Execute a SQL query using the embedded DuckDB engine",
    mixinStandardHelpOptions = true)
public class QueryCommand implements Callable<Integer> {
  private static final Logger LOGGER = LogManager.getLogger(QueryCommand.class);

  @Parameters(index = "0", description = "The SQL query to execute")
  private String sqlQuery;

  @Option(
      names = {"-f", "--file"},
      description = "Optional data file path to query (Parquet, CSV, etc.)")
  private String dataFile;

  @Option(
      names = {"-v", "--view"},
      description = "View name to create from the file (default: 'data')",
      defaultValue = "data")
  private String viewName;

  @Option(
      names = {"-d", "--database"},
      description = "Database file path (default: in-memory)",
      defaultValue = ":memory:")
  private String databasePath;

  @Option(
      names = {"--format"},
      description = "Output format: table, json, csv (default: table)",
      defaultValue = "table")
  private String outputFormat;

  @Override
  public Integer call() {
    // CLI creates its own engine instance with direct JDBC connection
    // This is acceptable for CLI usage as it's not a long-running server
    try {
      DuckDBEngine engine = new DuckDBEngine();
      // Manually set the database path for CLI usage
      EngineConfig cliEngineConfig =
          new EngineConfig() {
            @Override
            public String type() {
              return "duckdb";
            }

            @Override
            public String databasePath() {
              return databasePath;
            }

            @Override
            public java.util.Optional<DuckDBConfig> duckdb() {
              return java.util.Optional.empty();
            }
          };

      // Create a simple connection pool for CLI
      DuckDBConnectionPool pool = new DuckDBConnectionPool();
      pool.setEngineConfig(cliEngineConfig);
      pool.setPoolConfig(
          new io.github.cogitatortech.tamarind.config.ConnectionPoolConfig() {
            @Override
            public Integer maximumPoolSize() {
              return 1;
            }

            @Override
            public Integer minimumIdle() {
              return 0;
            }

            @Override
            public Long connectionTimeout() {
              return 30000L;
            }

            @Override
            public Long idleTimeout() {
              return 600000L;
            }

            @Override
            public Long maxLifetime() {
              return 1800000L;
            }

            @Override
            public String poolName() {
              return "CLIPool";
            }
          });

      // Initialize the pool
      pool.initialize();
      engine.connectionPool = pool;
      engine.engineConfig = cliEngineConfig;
      engine.initialize();

      try {
        if (dataFile != null && !dataFile.isEmpty()) {
          LOGGER.info("Creating view '{}' from file: {}", viewName, dataFile);
          engine.createViewFromFile(viewName, dataFile);
        }

        LOGGER.info("Executing query: {}", sqlQuery);
        QueryResult result = engine.executeQuery(sqlQuery);

        displayResults(result);

        System.out.println("\nQuery completed successfully. Rows: " + result.getRowCount());
        return 0;
      } finally {
        pool.cleanup();
      }
    } catch (SQLException e) {
      LOGGER.error("Query execution failed", e);
      System.err.println("Error executing query: " + e.getMessage());
      return 1;
    }
  }

  private void displayResults(QueryResult result) {
    switch (outputFormat.toLowerCase()) {
      case "json":
        displayAsJson(result);
        break;
      case "csv":
        displayAsCsv(result);
        break;
      case "table":
      default:
        displayAsTable(result);
        break;
    }
  }

  private void displayAsTable(QueryResult result) {
    if (result.getRowCount() == 0) {
      System.out.println("No results returned.");
      return;
    }

    int[] columnWidths = new int[result.getColumnCount()];
    for (int i = 0; i < result.getColumnCount(); i++) {
      String colName = result.getColumnNames().get(i);
      columnWidths[i] = colName.length();
    }

    for (Map<String, Object> row : result.getRows()) {
      int i = 0;
      for (String colName : result.getColumnNames()) {
        Object value = row.get(colName);
        String valueStr = value != null ? value.toString() : "NULL";
        columnWidths[i] = Math.max(columnWidths[i], valueStr.length());
        i++;
      }
    }

    System.out.println();
    printSeparator(columnWidths);
    printRow(result.getColumnNames(), columnWidths);
    printSeparator(columnWidths);

    for (Map<String, Object> row : result.getRows()) {
      String[] values = new String[result.getColumnCount()];
      for (int i = 0; i < result.getColumnCount(); i++) {
        String colName = result.getColumnNames().get(i);
        Object value = row.get(colName);
        values[i] = value != null ? value.toString() : "NULL";
      }
      printRow(values, columnWidths);
    }
    printSeparator(columnWidths);
  }

  private void printSeparator(int[] columnWidths) {
    System.out.print("+");
    for (int width : columnWidths) {
      System.out.print("-".repeat(width + 2) + "+");
    }
    System.out.println();
  }

  private void printRow(java.util.List<String> values, int[] columnWidths) {
    printRow(values.toArray(new String[0]), columnWidths);
  }

  private void printRow(String[] values, int[] columnWidths) {
    System.out.print("|");
    for (int i = 0; i < values.length; i++) {
      System.out.print(" " + padRight(values[i], columnWidths[i]) + " |");
    }
    System.out.println();
  }

  private String padRight(String s, int n) {
    return String.format("%-" + n + "s", s);
  }

  private void displayAsJson(QueryResult result) {
    System.out.println("[");
    for (int i = 0; i < result.getRows().size(); i++) {
      Map<String, Object> row = result.getRows().get(i);
      System.out.print("  {");
      int j = 0;
      for (Map.Entry<String, Object> entry : row.entrySet()) {
        String value;
        if (entry.getValue() == null) {
          value = "null";
        } else if (entry.getValue() instanceof Number) {
          value = entry.getValue().toString();
        } else if (entry.getValue() instanceof Boolean) {
          value = entry.getValue().toString();
        } else {
          // Escape JSON string values
          value = "\"" + escapeJsonString(entry.getValue().toString()) + "\"";
        }
        System.out.print("\"" + entry.getKey() + "\": " + value);
        if (j < row.size() - 1) {
          System.out.print(", ");
        }
        j++;
      }
      System.out.print("}");
      if (i < result.getRows().size() - 1) {
        System.out.println(",");
      } else {
        System.out.println();
      }
    }
    System.out.println("]");
  }

  private String escapeJsonString(String s) {
    if (s == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray()) {
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < ' ') {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.toString();
  }

  private void displayAsCsv(QueryResult result) {
    System.out.println(String.join(",", result.getColumnNames()));

    for (Map<String, Object> row : result.getRows()) {
      String[] values = new String[result.getColumnCount()];
      for (int i = 0; i < result.getColumnCount(); i++) {
        String colName = result.getColumnNames().get(i);
        Object value = row.get(colName);
        values[i] = escapeCsvValue(value != null ? value.toString() : "");
      }
      System.out.println(String.join(",", values));
    }
  }

  private String escapeCsvValue(String value) {
    if (value == null) {
      return "";
    }
    // If value contains comma, quote, or newline, escape it
    if (value.contains(",")
        || value.contains("\"")
        || value.contains("\n")
        || value.contains("\r")) {
      // Escape quotes by doubling them
      value = value.replace("\"", "\"\"");
      // Wrap in quotes
      return "\"" + value + "\"";
    }
    return value;
  }
}
