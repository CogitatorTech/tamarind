package io.github.cogitatortech.tamarind.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.github.cogitatortech.tamarind.cache.MetadataCache;
import io.github.cogitatortech.tamarind.config.DataConfig;
import io.github.cogitatortech.tamarind.engine.QueryEngine;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for DataInitializer. */
class DataInitializerTest {

  private DataInitializer initializer;

  @Mock private QueryEngine engine;
  @Mock private DataConfig config;
  @Mock private MetadataCache cache;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    initializer = new DataInitializer();
    initializer.engine = engine;
    initializer.config = config;
    initializer.cache = cache;
  }

  @Test
  void testInitWithNullSource() {
    when(config.source()).thenReturn(null);

    // Should not throw exception
    assertDoesNotThrow(() -> initializer.init());
  }

  @Test
  void testInitWithEmptySource() {
    when(config.source()).thenReturn("");
    when(cache.getFileList("")).thenReturn(null);

    assertDoesNotThrow(() -> initializer.init());
  }

  @Test
  void testInitWithCachedFiles() throws SQLException {
    String source = "s3://bucket/data/";
    List<String> files = List.of("file1.parquet", "file2.parquet");

    when(config.source()).thenReturn(source);
    when(cache.getFileList(source)).thenReturn(files);

    initializer.init();

    // Should use cached files
    verify(cache).getFileList(source);
    verify(engine, times(2)).createViewFromFile(anyString(), anyString());
  }

  @Test
  void testInitHandlesSQLException() throws SQLException {
    String source = "s3://bucket/data/";
    when(config.source()).thenReturn(source);
    when(cache.getFileList(source)).thenReturn(List.of("test.parquet"));
    doThrow(new SQLException("Connection error")).when(engine).createViewFromFile(any(), any());

    // Should log error but not throw
    assertDoesNotThrow(() -> initializer.init());
  }

  @Test
  void testInitWithNullEngine() {
    initializer.engine = null;
    when(config.source()).thenReturn("local:/data/test.parquet");
    when(cache.getFileList(any())).thenReturn(List.of());

    // Should handle null engine gracefully
    assertDoesNotThrow(() -> initializer.init());
  }

  @Test
  void testInitWithNullConfig() {
    initializer.config = null;

    // Should handle null config gracefully - but it doesn't in current implementation
    // This test documents the current behavior
    assertThrows(NullPointerException.class, () -> initializer.init());
  }
}
