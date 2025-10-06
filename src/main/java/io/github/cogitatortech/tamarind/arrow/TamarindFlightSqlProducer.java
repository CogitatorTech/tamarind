package io.github.cogitatortech.tamarind.arrow;

import io.github.cogitatortech.tamarind.config.QueryConfig;
import io.github.cogitatortech.tamarind.engine.JdbcQueryEngine;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import org.apache.arrow.adapter.jdbc.*;
import org.apache.arrow.flight.*;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.arrow.vector.util.TransferPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Arrow Flight producer for Tamarind queries. Implements the Flight protocol to provide
 * high-performance data transfer.
 *
 * <p>Note: This implementation requires JdbcQueryEngine for direct JDBC access. Non-JDBC engines
 * are not supported by Arrow Flight.
 */
public class TamarindFlightSqlProducer implements FlightProducer {

  private static final Logger LOGGER = LogManager.getLogger(TamarindFlightSqlProducer.class);

  private final JdbcQueryEngine engine;
  private final BufferAllocator allocator;

  @Inject QueryConfig queryConfig;

  public TamarindFlightSqlProducer(JdbcQueryEngine engine, BufferAllocator allocator) {
    this.engine = engine;
    this.allocator = allocator;
    LOGGER.info("TamarindFlightSqlProducer initialized with {} engine", engine.getEngineName());
  }

  /** Apply query timeout to statement if configured. */
  private void applyQueryTimeout(PreparedStatement stmt) throws Exception {
    if (queryConfig != null && queryConfig.timeoutSeconds().isPresent()) {
      int timeout = queryConfig.timeoutSeconds().get();
      stmt.setQueryTimeout(timeout);
      LOGGER.debug("Query timeout set to {} seconds", timeout);
    }
  }

  @Override
  public void getStream(CallContext context, Ticket ticket, ServerStreamListener listener) {
    String sql = new String(ticket.getBytes());
    LOGGER.info("Streaming query: {}", sql);

    try (Connection conn = engine.getJdbcConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      // Apply query timeout and enable streaming from JDBC
      applyQueryTimeout(stmt);
      try {
        stmt.setFetchSize(1000);
      } catch (Exception ignore) {
        // Not all drivers support fetch size
      }

      try (ResultSet resultSet = stmt.executeQuery()) {

        // Configure JDBC to Arrow conversion
        JdbcToArrowConfig config =
            new JdbcToArrowConfigBuilder(allocator, null).setTargetBatchSize(1024).build();

        // Get schema first
        Schema schema = JdbcToArrowUtils.jdbcToArrowSchema(resultSet.getMetaData(), config);

        // Create vector schema root
        try (VectorSchemaRoot root = VectorSchemaRoot.create(schema, allocator)) {
          listener.start(root);

          // Stream data in batches using the iterator
          ArrowVectorIterator iterator = JdbcToArrow.sqlToArrowVectorIterator(resultSet, config);
          while (iterator.hasNext()) {
            try (VectorSchemaRoot batch = iterator.next()) {
              // Transfer data from batch to root using transfer pairs to avoid copy bugs
              for (int i = 0; i < batch.getFieldVectors().size(); i++) {
                TransferPair tp = batch.getVector(i).makeTransferPair(root.getVector(i));
                tp.transfer();
              }
              root.setRowCount(batch.getRowCount());

              // Send batch to client
              listener.putNext();
            }
          }

          listener.completed();
        }

        LOGGER.info("Query streaming completed successfully");

      } catch (Exception e) {
        LOGGER.error("Error streaming query results", e);
        listener.error(
            CallStatus.INTERNAL
                .withDescription("Error executing query: " + e.getMessage())
                .toRuntimeException());
      }
    } catch (Exception e) {
      LOGGER.error("Error preparing statement", e);
      listener.error(
          CallStatus.INTERNAL
              .withDescription("Error preparing statement: " + e.getMessage())
              .toRuntimeException());
    }
  }

  @Override
  public void listFlights(
      CallContext context, Criteria criteria, StreamListener<FlightInfo> listener) {
    LOGGER.debug("listFlights called");
    listener.onCompleted();
  }

  @Override
  public FlightInfo getFlightInfo(CallContext context, FlightDescriptor descriptor) {
    byte[] cmd = descriptor.getCommand();
    String sql = new String(cmd);
    LOGGER.info("getFlightInfo for query: {}", sql);

    try (Connection conn = engine.getJdbcConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      // Apply query timeout
      applyQueryTimeout(stmt);

      try (ResultSet resultSet = stmt.executeQuery()) {

        // Get schema
        JdbcToArrowConfig config = new JdbcToArrowConfigBuilder(allocator, null).build();
        Schema schema = JdbcToArrowUtils.jdbcToArrowSchema(resultSet.getMetaData(), config);

        // Create flight info with empty location (means this server)
        FlightEndpoint endpoint = new FlightEndpoint(new Ticket(cmd));

        return new FlightInfo(
            schema,
            descriptor,
            List.of(endpoint),
            -1, // Unknown number of bytes
            -1 // Unknown number of rows
            );

      } catch (Exception e) {
        LOGGER.error("Error getting flight info", e);
        throw CallStatus.INTERNAL
            .withDescription("Error getting query schema: " + e.getMessage())
            .toRuntimeException();
      }
    } catch (Exception e) {
      LOGGER.error("Error preparing statement", e);
      throw CallStatus.INTERNAL
          .withDescription("Error preparing statement: " + e.getMessage())
          .toRuntimeException();
    }
  }

  @Override
  public Runnable acceptPut(
      CallContext context, FlightStream flightStream, StreamListener<PutResult> ackStream) {
    return () -> {
      LOGGER.warn("acceptPut not implemented");
      ackStream.onCompleted();
    };
  }

  @Override
  public void doAction(CallContext context, Action action, StreamListener<Result> listener) {
    LOGGER.warn("doAction not implemented for action: {}", action.getType());
    listener.onCompleted();
  }

  @Override
  public void listActions(CallContext context, StreamListener<ActionType> listener) {
    LOGGER.debug("listActions called");
    listener.onCompleted();
  }

  @Override
  public SchemaResult getSchema(CallContext context, FlightDescriptor descriptor) {
    byte[] cmd = descriptor.getCommand();
    String sql = new String(cmd);
    LOGGER.info("getSchema for query: {}", sql);

    try (Connection conn = engine.getJdbcConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      // Apply query timeout
      applyQueryTimeout(stmt);

      try (ResultSet resultSet = stmt.executeQuery()) {

        // Get schema
        JdbcToArrowConfig config = new JdbcToArrowConfigBuilder(allocator, null).build();
        Schema schema = JdbcToArrowUtils.jdbcToArrowSchema(resultSet.getMetaData(), config);

        return new SchemaResult(schema);

      } catch (Exception e) {
        LOGGER.error("Error getting schema", e);
        throw CallStatus.INTERNAL
            .withDescription("Error getting query schema: " + e.getMessage())
            .toRuntimeException();
      }
    } catch (Exception e) {
      LOGGER.error("Error preparing statement", e);
      throw CallStatus.INTERNAL
          .withDescription("Error preparing statement: " + e.getMessage())
          .toRuntimeException();
    }
  }
}
