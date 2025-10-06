package io.github.cogitatortech.tamarind.arrow;

import io.github.cogitatortech.tamarind.config.ArrowConfig;
import io.github.cogitatortech.tamarind.engine.JdbcQueryEngine;
import io.github.cogitatortech.tamarind.engine.QueryEngine;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.arrow.flight.FlightServer;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service to start and manage the Arrow Flight server. It provides high-performance data transfer
 * using the Arrow Flight protocol.
 */
@ApplicationScoped
public class ArrowFlightServerService {

  private static final Logger LOGGER = LogManager.getLogger(ArrowFlightServerService.class);

  @Inject QueryEngine engine;
  @Inject ArrowConfig arrowConfig;

  private FlightServer flightServer;
  private BufferAllocator allocator;

  private static boolean isBindException(Throwable e) {
    Throwable cur = e;
    while (cur != null) {
      if (cur instanceof java.net.BindException) return true;
      cur = cur.getCause();
    }
    return false;
  }

  /** Starts the Arrow Flight server on application startup. */
  void onStart(@Observes StartupEvent event) {
    // Always skip in Quarkus TEST mode to avoid port binding during unit and integration testing
    if (LaunchMode.current() == LaunchMode.TEST) {
      LOGGER.info("Skipping Arrow Flight server startup in test mode");
      return;
    }

    if (!arrowConfig.enabled()) {
      LOGGER.info("Arrow Flight server is disabled");
      return;
    }

    try {
      String host = arrowConfig.host();
      int port = arrowConfig.port();
      long maxMemory = arrowConfig.allocatorMaxMemory();

      LOGGER.info(
          "Starting Arrow Flight server on {}:{} with {} engine",
          host,
          port,
          engine.getEngineName());

      // Create root allocator for Arrow memory management
      allocator = new RootAllocator(maxMemory);

      // Arrow Flight requires JDBC access, so check if engine supports it
      if (!(engine instanceof JdbcQueryEngine)) {
        LOGGER.warn(
            "Arrow Flight server requires JdbcQueryEngine, but got {}. Skipping Arrow Flight initialization.",
            engine.getClass().getName());
        return;
      }

      // Create the Flight producer - now works with JdbcQueryEngine
      TamarindFlightSqlProducer producer =
          new TamarindFlightSqlProducer((JdbcQueryEngine) engine, allocator);

      Location location;
      ArrowConfig.TlsConfig tls = arrowConfig.tls();
      if (tls != null && Boolean.TRUE.equals(tls.enabled())) {
        location = Location.forGrpcTls(host, port);

        Optional<String> cert = tls.certFile();
        Optional<String> key = tls.keyFile();
        if (cert.isEmpty() || key.isEmpty()) {
          throw new IllegalStateException("TLS is enabled but certFile/keyFile are not configured");
        }
        if (!Files.exists(Path.of(cert.get())) || !Files.exists(Path.of(key.get()))) {
          throw new IllegalStateException("TLS is enabled but certFile/keyFile paths do not exist");
        }
        LOGGER.info("Starting Flight server with TLS enabled on {}:{}", host, port);
      } else {
        location = Location.forGrpcInsecure(host, port);
      }

      // Build and start the Flight server
      try {
        flightServer = FlightServer.builder(allocator, location, producer).build().start();
      } catch (Exception e) {
        // If port is in use during tests, don't fail the app startup
        if (LaunchMode.current() == LaunchMode.TEST && isBindException(e)) {
          LOGGER.warn(
              "Arrow Flight port {} is in use in tests. Skipping Flight server startup.", port, e);
          safeCloseAllocator();
          return;
        }
        throw e;
      }

      LOGGER.info("Arrow Flight server started successfully on port {}", port);
      LOGGER.info(
          "Connect using: pyarrow.flight.connect('{}://{}:{}')",
          (tls != null && Boolean.TRUE.equals(tls.enabled())) ? "grpc+tls" : "grpc",
          host,
          port);

    } catch (Exception e) {
      LOGGER.error("Failed to start Arrow Flight server", e);
      throw new RuntimeException("Failed to start Arrow Flight server", e);
    }
  }

  private void safeCloseAllocator() {
    try {
      if (allocator != null) {
        allocator.close();
      }
    } catch (Exception ignore) {
      // no-op
    } finally {
      allocator = null;
    }
  }

  /** Stops the Arrow Flight server on application shutdown. */
  void onStop(@Observes ShutdownEvent event) {
    try {
      if (flightServer != null) {
        LOGGER.info("Stopping Arrow Flight server...");
        flightServer.close();
        LOGGER.info("Arrow Flight server stopped");
      }

      if (allocator != null) {
        allocator.close();
        LOGGER.info("Arrow allocator closed");
      }

    } catch (Exception e) {
      LOGGER.error("Error stopping Arrow Flight server", e);
    }
  }
}
