package io.github.cogitatortech.tamarind.cache;

import static org.junit.jupiter.api.Assertions.*;

import io.github.cogitatortech.tamarind.engine.QueryResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QueryResultCacheTest {

  private QueryResultCache cache;

  @BeforeEach
  public void setUp() {
    cache = new QueryResultCache();
    cache.initialize();
  }

  @Test
  public void putAndGet_returnsSameResult() {
    String sql = "SELECT * FROM t";
    QueryResult r = new QueryResult(List.of("a"), List.of(Map.of("a", 1)));

    assertNull(cache.get(null));

    cache.put(sql, r);
    QueryResult got = cache.get(sql);
    assertNotNull(got);
    assertEquals(1, got.getRowCount());
    assertEquals(1, got.getColumnCount());
  }

  @Test
  public void put_nullResult_isNoop() {
    String sql = "SELECT 1";
    cache.put(sql, null);
    assertNull(cache.get(sql));
  }

  @Test
  public void getStats_and_hitRate_doNotThrow() {
    String stats = cache.getStats();
    double hr = cache.getHitRate();
    assertNotNull(stats);
    assertTrue(hr >= 0.0 && hr <= 1.0);
  }
}
