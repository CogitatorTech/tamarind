package io.github.cogitatortech.tamarind.service;

import io.github.cogitatortech.tamarind.cache.MetadataCache;
import io.github.cogitatortech.tamarind.config.DataConfig;
import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/** Service for initializing data sources and creating views on startup. */
@ApplicationScoped
public class DataInitializer {

  private static final Logger LOGGER = LogManager.getLogger(DataInitializer.class);

  @Inject DataConfig config;

  @Inject QueryEngine engine;

  @Inject MetadataCache cache;

  private volatile boolean initialized = false;

  void onStart(@Observes StartupEvent event) {
    // Start async initialization - don't block startup
    CompletableFuture.runAsync(this::init)
        .exceptionally(
            throwable -> {
              LOGGER.error("Failed to initialize data sources", throwable);
              return null;
            });
    LOGGER.info("Data initialization started asynchronously");
  }

  public void init() {
    String source = config.source();

    // Handle null or empty source
    if (source == null || source.trim().isEmpty()) {
      LOGGER.info("No data source configured, skipping data initialization");
      initialized = true;
      return;
    }

    LOGGER.info("Initializing data from source: {}", source);

    // Check cache first
    List<String> files = cache.getFileList(source);
    if (files == null) {
      files = scan(source);
      if (files == null || files.isEmpty()) {
        LOGGER.info("No data files found in source: {}", source);
        initialized = true;
        return;
      }
      cache.putFileList(source, files);
    } else {
      LOGGER.info("Using cached file list for: {}", source);
    }

    int successCount = 0;
    for (String file : files) {
      String viewName = getViewName(file);
      try {
        engine.createViewFromFile(viewName, file);
        successCount++;
      } catch (SQLException e) {
        LOGGER.error("Failed to create view '{}' for file {}", viewName, file, e);
      }
    }
    LOGGER.info("Data initialization complete. Created {} views.", successCount);
    initialized = true;
  }

  public boolean isInitialized() {
    return initialized;
  }

  private List<String> scan(String source) {
    if (source.startsWith("s3://")) {
      return scanS3(source);
    } else if (source.startsWith("local:")) {
      return scanLocal(Path.of(source.substring(6)));
    } else {
      return scanLocal(Path.of(source));
    }
  }

  private List<String> scanLocal(Path path) {
    try (Stream<Path> paths = Files.walk(path)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".parquet") || p.toString().endsWith(".csv"))
          .map(Path::toString)
          .collect(Collectors.toList());
    } catch (IOException e) {
      LOGGER.error("Failed to scan local path {}", path, e);
      return List.of();
    }
  }

  private List<String> scanS3(String s3Path) {
    String path = s3Path.substring(5); // remove s3://
    int slash = path.indexOf('/');
    String bucket = path.substring(0, slash);
    String prefix = path.substring(slash + 1);

    try (S3Client s3Client = S3Client.builder().build()) {
      ListObjectsV2Request request =
          ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
      ListObjectsV2Response response = s3Client.listObjectsV2(request);
      return response.contents().stream()
          .map(S3Object::key)
          .filter(key -> key.endsWith(".parquet") || key.endsWith(".csv"))
          .map(key -> "s3://" + bucket + "/" + key)
          .collect(Collectors.toList());
    } catch (Exception e) {
      LOGGER.error("Failed to scan S3 path {}", s3Path, e);
      return List.of();
    }
  }

  private String getViewName(String filePath) {
    String name = Paths.get(filePath).getFileName().toString();
    int dot = name.lastIndexOf('.');
    if (dot > 0) name = name.substring(0, dot);
    return name.replaceAll("[^a-zA-Z0-9_]", "_");
  }
}
