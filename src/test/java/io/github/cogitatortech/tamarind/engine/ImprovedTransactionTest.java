package io.github.cogitatortech.tamarind.engine;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/** Tests for improved transaction handling with TransactionContext. */
@QuarkusTest
class ImprovedTransactionTest {

  @Inject QueryEngine engine;

  @Test
  void testTransactionIsolation() throws SQLException {
    // Begin transaction
    engine.beginTransaction();
    assertTrue(engine.isTransactionActive(), "Transaction should be active");

    // Create table and insert data
    engine.executeUpdate("CREATE TABLE tx_test (id INTEGER, value VARCHAR)");
    engine.executeUpdate("INSERT INTO tx_test VALUES (1, 'test')");

    // Rollback
    engine.rollback();
    assertFalse(engine.isTransactionActive(), "Transaction should not be active after rollback");

    // Verify data was rolled back
    assertThrows(
        SQLException.class,
        () -> {
          engine.executeQuery("SELECT * FROM tx_test");
        },
        "Table should not exist after rollback");
  }

  @Test
  void testTransactionCommit() throws SQLException {
    // Begin transaction
    engine.beginTransaction();

    // Create table and insert data
    engine.executeUpdate("CREATE TABLE tx_commit_test (id INTEGER)");
    engine.executeUpdate("INSERT INTO tx_commit_test VALUES (1)");

    // Commit
    engine.commit();
    assertFalse(engine.isTransactionActive(), "Transaction should not be active after commit");

    // Verify data persists (in memory for :memory: database)
    QueryResult result = engine.executeQuery("SELECT * FROM tx_commit_test");
    assertEquals(1, result.getRowCount(), "Data should persist after commit");

    // Cleanup
    engine.executeUpdate("DROP TABLE tx_commit_test");
  }

  @Test
  void testNestedTransactionPrevention() throws SQLException {
    engine.beginTransaction();
    assertTrue(engine.isTransactionActive());

    // Attempting to start another transaction should fail
    assertThrows(
        SQLException.class,
        () -> {
          engine.beginTransaction();
        },
        "Should not allow nested transactions");

    // Cleanup
    engine.rollback();
  }

  @Test
  void testCommitWithoutActiveTransaction() {
    assertFalse(engine.isTransactionActive());

    assertThrows(
        SQLException.class,
        () -> {
          engine.commit();
        },
        "Should not allow commit without active transaction");
  }

  @Test
  void testRollbackWithoutActiveTransaction() {
    assertFalse(engine.isTransactionActive());

    assertThrows(
        SQLException.class,
        () -> {
          engine.rollback();
        },
        "Should not allow rollback without active transaction");
  }

  @Test
  void testQueriesOutsideTransaction() throws SQLException {
    assertFalse(engine.isTransactionActive());

    // Should work without transaction
    QueryResult result = engine.executeQuery("SELECT 1 as test");
    assertNotNull(result);
    assertEquals(1, result.getRowCount());
  }

  @Test
  void testTransactionWithMultipleOperations() throws SQLException {
    engine.beginTransaction();

    // Multiple operations in same transaction
    engine.executeUpdate("CREATE TABLE multi_op (id INTEGER)");
    engine.executeUpdate("INSERT INTO multi_op VALUES (1)");
    engine.executeUpdate("INSERT INTO multi_op VALUES (2)");
    engine.executeUpdate("INSERT INTO multi_op VALUES (3)");

    QueryResult result = engine.executeQuery("SELECT COUNT(*) as cnt FROM multi_op");
    assertEquals(3L, result.getRows().get(0).get("cnt"));

    engine.commit();

    // Verify after commit
    result = engine.executeQuery("SELECT COUNT(*) as cnt FROM multi_op");
    assertEquals(3L, result.getRows().get(0).get("cnt"));

    // Cleanup
    engine.executeUpdate("DROP TABLE multi_op");
  }
}
