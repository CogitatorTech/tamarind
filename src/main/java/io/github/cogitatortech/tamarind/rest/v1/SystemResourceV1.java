package io.github.cogitatortech.tamarind.rest.v1;

import io.github.cogitatortech.tamarind.storage.EphemeralRepository;
import io.github.cogitatortech.tamarind.storage.UserRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * REST endpoint for system information and status.
 *
 * <p>Provides information about: - Application version and status - Storage backend configuration -
 * JVM and runtime metrics - User and token counts
 */
@Path("/api/v1/system")
public class SystemResourceV1 {

  @Inject UserRepository userRepository;
  @Inject EphemeralRepository ephemeralRepository;

  @ConfigProperty(name = "quarkus.application.name")
  String applicationName;

  @ConfigProperty(name = "quarkus.application.version", defaultValue = "0.1.0-SNAPSHOT")
  String applicationVersion;

  /**
   * Get system information.
   *
   * @return system info including storage backends, metrics, and status
   */
  @GET
  @Path("/info")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getSystemInfo() {
    Map<String, Object> info = new HashMap<>();

    // Application info
    info.put("name", applicationName);
    info.put("version", applicationVersion);
    info.put("status", "running");
    info.put("timestamp", Instant.now().toString());

    // Storage backends
    Map<String, String> storage = new HashMap<>();
    storage.put("oltp_backend", getRepositoryType(userRepository));
    storage.put("ephemeral_backend", ephemeralRepository.getBackendName());
    info.put("storage", storage);

    // User statistics
    Map<String, Object> users = new HashMap<>();
    users.put("total_users", userRepository.count());
    info.put("users", users);

    // JVM info
    Map<String, Object> jvm = new HashMap<>();
    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    jvm.put("uptime_ms", runtimeMxBean.getUptime());
    jvm.put("start_time", Instant.ofEpochMilli(runtimeMxBean.getStartTime()).toString());

    MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
    Map<String, Object> memory = new HashMap<>();
    memory.put("heap_used_mb", memoryMxBean.getHeapMemoryUsage().getUsed() / 1024 / 1024);
    memory.put("heap_max_mb", memoryMxBean.getHeapMemoryUsage().getMax() / 1024 / 1024);
    memory.put("heap_committed_mb", memoryMxBean.getHeapMemoryUsage().getCommitted() / 1024 / 1024);
    jvm.put("memory", memory);

    info.put("jvm", jvm);

    return info;
  }

  /**
   * Get system health status.
   *
   * @return health status
   */
  @GET
  @Path("/health")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getHealth() {
    Map<String, Object> health = new HashMap<>();
    health.put("status", "UP");
    health.put("timestamp", Instant.now().toString());

    // Check storage backends
    Map<String, String> checks = new HashMap<>();
    try {
      long userCount = userRepository.count();
      checks.put("user_repository", "UP (users: " + userCount + ")");
    } catch (Exception e) {
      checks.put("user_repository", "DOWN: " + e.getMessage());
      health.put("status", "DEGRADED");
    }

    checks.put("ephemeral_repository", "UP (" + ephemeralRepository.getBackendName() + ")");

    health.put("checks", checks);
    return health;
  }

  /**
   * Get system configuration.
   *
   * @return current configuration settings
   */
  @GET
  @Path("/config")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getConfig() {
    Map<String, Object> config = new HashMap<>();

    // Storage configuration
    Map<String, Object> storage = new HashMap<>();
    storage.put("oltp_backend", getRepositoryType(userRepository));
    storage.put("ephemeral_backend", ephemeralRepository.getBackendName());
    config.put("storage", storage);

    // Application info
    config.put(
        "application",
        Map.of(
            "name", applicationName,
            "version", applicationVersion));

    return config;
  }

  /**
   * Get system statistics and metrics.
   *
   * @return system statistics including query counts, sessions, etc.
   */
  @GET
  @Path("/stats")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getStats() {
    Map<String, Object> stats = new HashMap<>();

    // User statistics
    stats.put("total_users", userRepository.count());

    // JVM statistics
    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();

    Map<String, Object> jvmStats = new HashMap<>();
    jvmStats.put("uptime_seconds", runtimeMxBean.getUptime() / 1000);
    jvmStats.put("heap_used_mb", memoryMxBean.getHeapMemoryUsage().getUsed() / 1024 / 1024);
    jvmStats.put("heap_max_mb", memoryMxBean.getHeapMemoryUsage().getMax() / 1024 / 1024);
    jvmStats.put(
        "heap_committed_mb", memoryMxBean.getHeapMemoryUsage().getCommitted() / 1024 / 1024);
    jvmStats.put(
        "heap_usage_percent",
        (memoryMxBean.getHeapMemoryUsage().getUsed() * 100.0)
            / memoryMxBean.getHeapMemoryUsage().getMax());

    // Non-heap memory (metaspace, etc.)
    jvmStats.put("non_heap_used_mb", memoryMxBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024);
    jvmStats.put("non_heap_max_mb", memoryMxBean.getNonHeapMemoryUsage().getMax() / 1024 / 1024);

    // Thread information
    var threadMxBean = ManagementFactory.getThreadMXBean();
    Map<String, Object> threadStats = new HashMap<>();
    threadStats.put("thread_count", threadMxBean.getThreadCount());
    threadStats.put("peak_thread_count", threadMxBean.getPeakThreadCount());
    threadStats.put("daemon_thread_count", threadMxBean.getDaemonThreadCount());
    threadStats.put("total_started_threads", threadMxBean.getTotalStartedThreadCount());
    jvmStats.put("threads", threadStats);

    // GC information
    var gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    List<Map<String, Object>> gcStats = new ArrayList<>();
    for (var gc : gcBeans) {
      Map<String, Object> gcInfo = new HashMap<>();
      gcInfo.put("name", gc.getName());
      gcInfo.put("collection_count", gc.getCollectionCount());
      gcInfo.put("collection_time_ms", gc.getCollectionTime());
      gcStats.add(gcInfo);
    }
    jvmStats.put("garbage_collectors", gcStats);

    stats.put("jvm", jvmStats);
    stats.put("timestamp", Instant.now().toString());

    return stats;
  }

  /** Get repository type name. */
  private String getRepositoryType(UserRepository repo) {
    String className = repo.getClass().getSimpleName();
    if (className.contains("InMemory")) {
      return "in-memory";
    } else if (className.contains("Jpa")) {
      return "database (JPA)";
    } else {
      return className;
    }
  }
}
