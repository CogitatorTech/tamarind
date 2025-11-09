package io.github.cogitatortech.tamarind.cache;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SqlNormalizerEdgeCasesTest {

  @Test
  public void nestedBlockComments_areRemoved() {
    String sql = "SELECT 1 /* outer /* nested */ still outer */ FROM dual";
    String normalized = SqlNormalizer.normalize(sql);
    assertFalse(normalized.contains("/*"));
    assertTrue(normalized.startsWith("select"));
  }

  @Test
  public void quotedIdentifiers_preservedCase_andLowercaseOutside() {
    String sql = "SELECT \"MyColumn\" FROM \"MyTable\" WHERE id=1";
    String normalized = SqlNormalizer.normalize(sql);
    // double-quoted identifiers should be preserved inside quotes
    assertTrue(normalized.contains("\"MyColumn\""));
    // outside quotes should be lowercased
    assertTrue(normalized.startsWith("select "));
  }

  @Test
  public void internationalCharacters_inStrings_andIdentifiers() {
    String sql = "SELECT \"名\" FROM \"テーブル\" WHERE name = 'Åke'";
    String normalized = SqlNormalizer.normalize(sql);
    // double-quoted identifier content preserved
    assertTrue(normalized.contains("\"名\""));
    // string literal replaced with placeholder '?'
    assertFalse(normalized.contains("Åke"));
    assertTrue(normalized.contains("?"));
  }
}
