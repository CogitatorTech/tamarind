package io.github.cogitatortech.tamarind.engine;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

import io.github.cogitatortech.tamarind.config.QueryConfig;
import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

public class DuckDBEngineCreateViewTest {

  @Test
  public void createViewFromFileUsesEscapedLiteralAndStatement() throws Exception {
    DuckDBConnectionPool pool = mock(DuckDBConnectionPool.class);
    Connection conn = mock(Connection.class);
    Statement stmt = mock(Statement.class);

    when(pool.getConnection()).thenReturn(conn);
    when(conn.createStatement()).thenReturn(stmt);

    DuckDBEngine engine = new DuckDBEngine();
    engine.connectionPool = pool; // inject mock
    engine.queryConfig = mock(QueryConfig.class);

    String viewName = "my_view";
    String filePath = "/tmp/data'special.csv"; // include a single quote to test escaping

    engine.createViewFromFile(viewName, filePath);

    // verify that the SQL executed contains the quoted view name and the escaped file path literal
    verify(conn).createStatement();
    verify(stmt).executeUpdate(contains("CREATE OR REPLACE VIEW"));
    verify(stmt).executeUpdate(contains("my_view"));
    verify(stmt).executeUpdate(contains("/tmp/data"));

    // verify resources closed
    verify(stmt).close();
    verify(conn).close();
  }
}
