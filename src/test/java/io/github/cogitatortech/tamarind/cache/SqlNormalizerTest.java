package io.github.cogitatortech.tamarind.cache;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SqlNormalizerTest {

  @Test
  public void normalize_removesCommentsAndWhitespace_andLowercases() {
    String sql = "SELECT  *  FROM MyTable -- inline comment\nWHERE col = 'A' /* block comment */";

    String normalized = SqlNormalizer.normalize(sql);

    // string literal placeholders are normalized to '?'
    assertEquals("select * from mytable where col = ?", normalized);
  }

  @Test
  public void generateCacheKey_isDeterministic_forEquivalentSql() {
    String a = "SELECT * FROM MyTable WHERE col='A'";
    String b = "  select *  from MYTABLE where col= 'A'  ";

    String k1 = SqlNormalizer.generateCacheKey(a);
    String k2 = SqlNormalizer.generateCacheKey(b);

    assertNotNull(k1);
    assertNotNull(k2);
    assertEquals(k1, k2);
    assertTrue(k1.length() > 0);
  }
}
