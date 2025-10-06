package io.github.cogitatortech.tamarind.engine;

/**
 * Marker interface for analytical query engines (OLAP databases).
 *
 * <p>Analytical engines are optimized for:
 *
 * <ul>
 *   <li>Columnar storage
 *   <li>Complex aggregations
 *   <li>Large-scale data scanning
 *   <li>Direct file reading (Parquet, CSV, JSON)
 * </ul>
 *
 * <p>Examples: DuckDB, ClickHouse, Apache Druid
 *
 * <p>This interface distinguishes analytical engines from OLTP databases, ensuring Tamarind focuses
 * on its core lakehouse use case.
 */
public interface AnalyticalQueryEngine extends QueryEngine {

  /**
   * Check if the engine supports direct file reading.
   *
   * @return true if engine can query files directly without loading
   */
  default boolean supportsDirectFileQuery() {
    return true;
  }

  /**
   * Get supported file formats for direct querying.
   *
   * @return array of supported formats (e.g., "parquet", "csv", "json")
   */
  String[] getSupportedFileFormats();

  /**
   * Check if the engine supports columnar operations.
   *
   * @return true if engine uses columnar storage/processing
   */
  default boolean isColumnar() {
    return true;
  }
}
