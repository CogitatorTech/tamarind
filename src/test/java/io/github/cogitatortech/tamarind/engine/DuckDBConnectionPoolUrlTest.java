package io.github.cogitatortech.tamarind.engine;

import static org.junit.jupiter.api.Assertions.*;

import io.github.cogitatortech.tamarind.config.ConnectionPoolConfig;
import io.github.cogitatortech.tamarind.config.EngineConfig;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DuckDBConnectionPoolUrlTest {

  private Path tempDir;
  private Path dbPath;
  private DuckDBConnectionPool pool;

  private static void setField(Object target, String name, Object value) throws Exception {
    Field f = DuckDBConnectionPool.class.getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  @BeforeEach
  void setUp() throws Exception {
    tempDir = Files.createTempDirectory("duckdb-test-");
    dbPath = tempDir.resolve("test.duckdb");

    pool = new DuckDBConnectionPool();

    // Inject fake configs via reflection
    setField(pool, "poolConfig", new FakePoolConfig());
    setField(pool, "engineConfig", new FakeEngineConfig(dbPath.toString()));

    pool.initialize();
  }

  @AfterEach
  void tearDown() {
    if (pool != null) {
      pool.cleanup();
    }
  }

  @Test
  void shouldNotCreateStrayUrlParameterFile() throws Exception {
    // Primary test: If URL parameters were appended incorrectly (e.g., as part of the file path),
    // DuckDB would create a file literally named "?access_mode=read_write&threads=4" or
    // "test.duckdb?access_mode=read_write&threads=4" next to the database file.
    Path strayWithQuestion = tempDir.resolve("?access_mode=read_write&threads=4");
    Path strayAsFilename = tempDir.resolve("test.duckdb?access_mode=read_write&threads=4");

    assertFalse(
        Files.exists(strayWithQuestion), "Stray parameter file created: " + strayWithQuestion);
    assertFalse(Files.exists(strayAsFilename), "Stray parameter file created: " + strayAsFilename);

    // Perform database operations to ensure parameters are actually being used
    try (var conn = pool.getConnection();
        Statement st = conn.createStatement()) {
      assertNotNull(conn, "Connection should be obtainable");

      // Create table and insert data
      st.executeUpdate("CREATE TABLE IF NOT EXISTS _t(i INT)");
      st.executeUpdate("INSERT INTO _t VALUES (1), (2), (3)");

      // Verify data is readable (proves connection is working)
      var rs = st.executeQuery("SELECT COUNT(*) as cnt FROM _t");
      assertTrue(rs.next(), "Query should return result");
      assertEquals(3, rs.getInt("cnt"), "Should have inserted 3 rows");
    }

    // The critical assertion: no stray parameter files should exist after operations
    assertFalse(
        Files.exists(strayWithQuestion),
        "Stray parameter file should not exist after database operations: " + strayWithQuestion);
    assertFalse(
        Files.exists(strayAsFilename),
        "Stray filename with parameters should not exist: " + strayAsFilename);

    // List all files in temp dir for debugging if test fails
    if (Files.exists(strayWithQuestion) || Files.exists(strayAsFilename)) {
      System.err.println("Files in temp dir:");
      try (var stream = Files.list(tempDir)) {
        stream.forEach(p -> System.err.println("  " + p.getFileName()));
      }
    }
  }

  // Minimal fakes for config interfaces
  static class FakePoolConfig implements ConnectionPoolConfig {
    public Integer maximumPoolSize() {
      return 2;
    }

    public Integer minimumIdle() {
      return 1;
    }

    public Long connectionTimeout() {
      return 30_000L;
    }

    public Long idleTimeout() {
      return 60_000L;
    }

    public Long maxLifetime() {
      return 120_000L;
    }

    public String poolName() {
      return "DuckDBPool-Test";
    }
  }

  static class FakeEngineConfig implements EngineConfig {
    private final String path;

    FakeEngineConfig(String path) {
      this.path = path;
    }

    public String type() {
      return "duckdb";
    }

    public String databasePath() {
      return path;
    }

    public Optional<DuckDBConfig> duckdb() {
      return Optional.of(new FakeDuck());
    }

    static class FakeDuck implements DuckDBConfig {
      public String accessMode() {
        return "read_write";
      }

      public Integer threads() {
        return 4;
      }

      public Boolean installHttpfs() {
        return false;
      }

      public Boolean loadHttpfs() {
        return false;
      }
    }
  }
}
