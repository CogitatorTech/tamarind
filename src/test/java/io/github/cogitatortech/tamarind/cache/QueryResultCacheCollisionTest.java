package io.github.cogitatortech.tamarind.cache;

import static org.junit.jupiter.api.Assertions.*;

import io.github.cogitatortech.tamarind.engine.QueryResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QueryResultCacheCollisionTest {

  private QueryResultCache cache;

  @BeforeEach
  public void setUp() {
    cache = new QueryResultCache();
    cache.initialize();
  }

  @Test
  public void differentLiterals_shouldNotCollide() {
    String sqlA = "SELECT * FROM t WHERE id = 1";
    String sqlB = "SELECT * FROM t WHERE id = 2";

    QueryResult r1 = new QueryResult(List.of("id"), List.of(Map.of("id", 1)));
    cache.put(sqlA, r1);

    assertNull(cache.get(sqlB));
    QueryResult r2 = new QueryResult(List.of("id"), List.of(Map.of("id", 2)));
    cache.put(sqlB, r2);

    assertNotSame(cache.get(sqlA), cache.get(sqlB));
  }
}
