/*
 * Copyright (c) 2012-2019 Snowflake Computing Inc. All right reserved.
 */
package net.snowflake.client.jdbc;

import net.snowflake.client.ConditionalIgnoreRule;
import net.snowflake.client.RunningOnTravisCI;
import net.snowflake.client.core.SFSession;
import net.snowflake.common.core.SqlState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Multi Statement tests
 */
@RunWith(Parameterized.class)
public class MultiStatementIT extends BaseJDBCTest
{
  @Parameterized.Parameters
  public static Object[][] data()
  {
    // all tests in this class need to run for both query result formats json and arrow
    return new Object[][]{
        {"JSON"}
        // , {"Arrow"}
    };
  }

  private static String queryResultFormat;

  public MultiStatementIT(String format)
  {
    queryResultFormat = format;
  }

  public static Connection getConnection()
  throws SQLException
  {
    Connection conn = BaseJDBCTest.getConnection();
    // conn.createStatement().execute("alter session set query_result_format = '" + queryResultFormat + "'");
    return conn;
  }

  private void enableMultiStmt(Connection connection) throws SQLException
  {
    Statement statement = connection.createStatement();
    statement.execute("alter session set ENABLE_MULTISTATEMENT=true");
    statement.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtTransaction() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();

    statement.execute("create or replace table test_multi_txn(c1 number, c2 string)" +
                      " as select 10, 'z'");

    String multiStmtQuery = "begin;\n" +
                            "delete from test_multi_txn;\n" +
                            "insert into test_multi_txn values (1, 'a'), (2, 'b');\n" +
                            "commit";

    boolean hasResultSet = statement.execute(multiStmtQuery);
    // first statement
    assertFalse(hasResultSet);
    assertNull(statement.getResultSet());
    assertEquals(-1, statement.getUpdateCount());

    // second statement
    assertFalse(statement.getMoreResults());
    assertNull(statement.getResultSet());
    assertEquals(1, statement.getUpdateCount());

    // third statement
    assertFalse(statement.getMoreResults());
    assertNull(statement.getResultSet());
    assertEquals(2, statement.getUpdateCount());

    // fourth statement
    assertFalse(statement.getMoreResults());
    assertNull(statement.getResultSet());
    assertEquals(-1, statement.getUpdateCount());

    assertFalse(statement.getMoreResults());
    assertEquals(-1, statement.getUpdateCount());

    statement.execute("drop table if exists test_multi_txn");
    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtTransactionRollback() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();

    statement.execute("create or replace table test_multi_txn_rb(c1 number, c2 string)" +
                      " as select 10, 'z'");

    String multiStmtQuery = "begin;\n" +
                            "delete from test_multi_txn_rb;\n" +
                            "rollback;\n" +
                            "select count(*) from test_multi_txn_rb";

    boolean hasResultSet = statement.execute(multiStmtQuery);
    // first statement
    assertFalse(hasResultSet);
    assertNull(statement.getResultSet());
    assertEquals(-1, statement.getUpdateCount());

    // second statement
    assertFalse(statement.getMoreResults());
    assertNull(statement.getResultSet());
    assertEquals(1, statement.getUpdateCount());

    // third statement
    assertFalse(statement.getMoreResults());
    assertNull(statement.getResultSet());
    assertEquals(-1, statement.getUpdateCount());

    // fourth statement
    assertTrue(statement.getMoreResults());
    assertEquals(-1, statement.getUpdateCount());
    ResultSet rs = statement.getResultSet();
    assertTrue(rs.next());
    assertEquals(1, rs.getInt(1));
    assertFalse(rs.next());

    assertFalse(statement.getMoreResults());
    assertEquals(-1, statement.getUpdateCount());

    statement.execute("drop table if exists test_multi_txn_rb");
    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtExecute() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();
    String multiStmtQuery = "create or replace temporary table test_multi (cola int);\n" +
                            "insert into test_multi VALUES (1), (2);\n" +
                            "select cola from test_multi order by cola asc";

    boolean hasResultSet = statement.execute(multiStmtQuery);
    // first statement
    assertFalse(hasResultSet);
    assertNull(statement.getResultSet());
    assertEquals(-1, statement.getUpdateCount());

    // second statement
    assertFalse(statement.getMoreResults());
    assertNull(statement.getResultSet());
    assertEquals(2, statement.getUpdateCount());

    // third statement
    assertTrue(statement.getMoreResults());
    assertEquals(-1, statement.getUpdateCount());
    ResultSet rs = statement.getResultSet();
    assertTrue(rs.next());
    assertEquals(1, rs.getInt(1));
    assertTrue(rs.next());
    assertEquals(2, rs.getInt(1));
    assertFalse(rs.next());

    assertFalse(statement.getMoreResults());
    assertEquals(-1, statement.getUpdateCount());

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtExecuteUpdate() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();
    String multiStmtQuery = "create or replace temporary table test_multi (cola int);\n" +
                            "insert into test_multi VALUES (1), (2);\n" +
                            "select cola from test_multi order by cola asc";

    int rowCount = statement.executeUpdate(multiStmtQuery);
    // first statement
    assertEquals(0, rowCount);
    assertNull(statement.getResultSet());
    assertEquals(-1, statement.getUpdateCount());

    // second statement
    assertFalse(statement.getMoreResults());
    assertNull(statement.getResultSet());
    assertEquals(2, statement.getUpdateCount());

    // third statement
    assertTrue(statement.getMoreResults());
    assertEquals(-1, statement.getUpdateCount());
    ResultSet rs = statement.getResultSet();
    assertTrue(rs.next());
    assertEquals(1, rs.getInt(1));
    assertTrue(rs.next());
    assertEquals(2, rs.getInt(1));
    assertFalse(rs.next());

    assertFalse(statement.getMoreResults());
    assertEquals(-1, statement.getUpdateCount());

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtExecuteQuery() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();
    String multiStmtQuery = "select 1;\n" +
                            "create or replace temporary table test_multi (cola int);\n" +
                            "insert into test_multi VALUES (1), (2);\n" +
                            "select cola from test_multi order by cola asc";

    ResultSet rs = statement.executeQuery(multiStmtQuery);
    // first statement
    assertNotNull(rs);
    assertNotNull(statement.getResultSet());
    assertEquals(-1, statement.getUpdateCount());
    assertTrue(rs.next());
    assertEquals(1, rs.getInt(1));
    assertFalse(rs.next());

    // second statement
    assertFalse(statement.getMoreResults());
    assertNull(statement.getResultSet());
    assertEquals(-1, statement.getUpdateCount());

    // third statement
    assertFalse(statement.getMoreResults());
    assertNull(statement.getResultSet());
    assertEquals(2, statement.getUpdateCount());

    // fourth statement
    assertTrue(statement.getMoreResults());
    assertEquals(-1, statement.getUpdateCount());
    rs = statement.getResultSet();
    assertTrue(rs.next());
    assertEquals(1, rs.getInt(1));
    assertTrue(rs.next());
    assertEquals(2, rs.getInt(1));
    assertFalse(rs.next());

    assertFalse(statement.getMoreResults());
    assertEquals(-1, statement.getUpdateCount());

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtExecuteUpdateFail() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();
    String multiStmtQuery = "select 1;\n" +
                            "create or replace temporary table test_multi (cola int);\n" +
                            "insert into test_multi VALUES (1), (2);\n" +
                            "select cola from test_multi order by cola asc";

    try
    {
      statement.executeUpdate(multiStmtQuery);
      fail("executeUpdate should have failed because the first statement yields a result set");
    }
    catch (SQLException ex)
    {
      assertThat(ex.getErrorCode(),
                 is(ErrorCode.UPDATE_FIRST_RESULT_NOT_UPDATE_COUNT.getMessageCode()));
    }

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtExecuteQueryFail() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();
    String multiStmtQuery = "create or replace temporary table test_multi (cola int);\n" +
                            "insert into test_multi VALUES (1), (2);\n" +
                            "select cola from test_multi order by cola asc";

    try
    {
      statement.executeQuery(multiStmtQuery);
      fail("executeQuery should have failed because the first statement yields an update count");
    }
    catch (SQLException ex)
    {
      assertThat(ex.getErrorCode(),
                 is(ErrorCode.QUERY_FIRST_RESULT_NOT_RESULT_SET.getMessageCode()));
    }

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtSetUnset() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();

    // setting session variable should propagate outside of query
    statement.execute("set testvar = 1; select 1");
    ResultSet rs = statement.executeQuery("select $testvar");
    rs.next();
    assertEquals(1, rs.getInt(1));

    // selecting unset variable should cause error
    try
    {
      statement.execute("unset testvar; select $testvar");
      fail("Expected a failure");
    }
    catch (SQLException ex)
    {
      assertEquals(SqlState.PLSQL_ERROR, ex.getSQLState());
    }

    // unsetting session variable should propagate outside of query
    try
    {
      statement.execute("select $testvar");
      fail("Expected a failure");
    }
    catch (SQLException ex)
    {
      assertEquals(SqlState.NO_DATA, ex.getSQLState());
    }

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtParseError() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();

    statement.execute("set testvar = 1");
    try
    {
      // fails in the antlr parser
      statement.execute("garbage text; set testvar = 2");
      fail("Expected a compiler error to be thrown");
    }
    catch (SQLException ex)
    {
      assertEquals(SqlState.SYNTAX_ERROR_OR_ACCESS_RULE_VIOLATION, ex.getSQLState());
    }

    ResultSet rs = statement.executeQuery("select $testvar");
    rs.next();
    assertEquals(1, rs.getInt(1));

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtExecError() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();

    try
    {
      // fails during execution (javascript invokes statement where it gets typechecked)
      statement.execute("set testvar = 1; select nonexistent_column from nonexistent_table; set testvar = 2");
      fail("Expected an execution error to be thrown");
    }
    catch (SQLException ex)
    {
      assertEquals(SqlState.PLSQL_ERROR, ex.getSQLState());
    }

    ResultSet rs = statement.executeQuery("select $testvar");
    rs.next();
    assertEquals(1, rs.getInt(1));

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtTempTable() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();

    String entry = "success";
    statement.execute(
        "create or replace temporary table test_multi (cola string); insert into test_multi values ('" + entry + "')");
    // temporary table should persist outside of the above statement
    ResultSet rs = statement.executeQuery("select * from test_multi");
    rs.next();
    assertEquals(entry, rs.getString(1));

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtUseStmt() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();

    statement.execute("use schema public; select 1");
    // current schema change should persist outside of the above statement

    SFSession session = statement.getConnection().unwrap(SnowflakeConnectionV1.class).getSfSession();
    assertEquals("PUBLIC", session.getSchema());
    ResultSet rs = statement.executeQuery("select current_schema()");
    rs.next();
    assertEquals("PUBLIC", rs.getString(1));

    statement.execute("use schema testschema; select 1");
    // current schema change should persist outside of the above statement

    session = statement.getConnection().unwrap(SnowflakeConnectionV1.class).getSfSession();
    assertEquals("TESTSCHEMA", session.getSchema());
    rs = statement.executeQuery("select current_schema()");
    rs.next();
    assertEquals("TESTSCHEMA", rs.getString(1));

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtAlterSessionParams() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();

    SFSession session = statement.getConnection().unwrap(SnowflakeConnectionV1.class).getSfSession();

    // we need an arbitrary parameter which is updated by the client after each query for this test
    String param = "AUTOCOMMIT";
    statement.execute("alter session set " + param + "=false; select 1");
    assertFalse(session.getAutoCommit());
    statement.execute("alter session set " + param + "=true; select 1");
    assertTrue(session.getAutoCommit());

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtMultiLine() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();

    // these statements should not fail
    statement.execute("select 1;\nselect 2");
    statement.execute("select \n 1; select 2");
    statement.execute("select \r\n 1; select 2");

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtQuotes() throws SQLException
  {
    // test various quotation usage and ensure they succeed
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();

    statement.execute("create or replace temporary table \"test_multi\" (cola string); select * from \"test_multi\"");
    statement.execute("create or replace temporary table `test_multi` (cola string); select * from `test_multi`");
    statement.execute("select 'str'; select 'str2'");
    statement.execute("select '\\` backticks'; select '\\\\` more `backticks`'");

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtCommitRollback() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();

    statement.execute("create or replace table test_multi (cola string)");
    statement.execute("begin");
    statement.execute("insert into test_multi values ('abc')");
    // "commit" inside multistatement commits previous DML calls
    statement.execute("insert into test_multi values ('def'); commit");
    statement.execute("rollback");
    ResultSet rs = statement.executeQuery("select count(*) from test_multi");
    assertTrue(rs.next());
    assertEquals(2, rs.getInt(1));

    statement.execute("create or replace table test_multi (cola string)");
    statement.execute("begin");
    statement.execute("insert into test_multi values ('abc')");
    // "rollback" inside multistatement rolls back previous DML calls
    statement.execute("insert into test_multi values ('def'); rollback");
    statement.execute("commit");
    rs = statement.executeQuery("select count(*) from test_multi");
    assertTrue(rs.next());
    assertEquals(0, rs.getInt(1));

    statement.execute("create or replace table test_multi (cola string)");
    // open transaction inside multistatement continues after
    statement.execute("begin; insert into test_multi values ('abc')");
    statement.execute("insert into test_multi values ('def')");
    statement.execute("commit");
    rs = statement.executeQuery("select count(*) from test_multi");
    assertTrue(rs.next());
    assertEquals(2, rs.getInt(1));

    statement.execute("create or replace table test_multi (cola string)");
    // open transaction inside multistatement continues after
    statement.execute("begin; insert into test_multi values ('abc')");
    statement.execute("insert into test_multi values ('def')");
    statement.execute("rollback");
    rs = statement.executeQuery("select count(*) from test_multi");
    assertTrue(rs.next());
    assertEquals(0, rs.getInt(1));

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtCommitRollbackNoAutocommit() throws SQLException
  {
    Connection connection = getConnection();
    enableMultiStmt(connection);
    connection.setAutoCommit(false);
    Statement statement = connection.createStatement();

    statement.execute("create or replace table test_multi (cola string)");
    statement.execute("insert into test_multi values ('abc')");
    // "commit" inside multistatement commits previous DML calls
    statement.execute("insert into test_multi values ('def'); commit");
    statement.execute("rollback");
    ResultSet rs = statement.executeQuery("select count(*) from test_multi");
    assertTrue(rs.next());
    assertEquals(2, rs.getInt(1));

    statement.execute("create or replace table test_multi (cola string)");
    statement.execute("insert into test_multi values ('abc')");
    // "rollback" inside multistatement rolls back previous DML calls
    statement.execute("insert into test_multi values ('def'); rollback");
    statement.execute("commit");
    rs = statement.executeQuery("select count(*) from test_multi");
    assertTrue(rs.next());
    assertEquals(0, rs.getInt(1));

    statement.execute("create or replace table test_multi (cola string)");
    // open transaction inside multistatement continues after
    statement.execute("insert into test_multi values ('abc'); insert into test_multi values ('def')");
    statement.execute("commit");
    rs = statement.executeQuery("select count(*) from test_multi");
    assertTrue(rs.next());
    assertEquals(2, rs.getInt(1));

    statement.execute("create or replace table test_multi (cola string)");
    // open transaction inside multistatement continues after
    statement.execute("insert into test_multi values ('abc'); insert into test_multi values ('def')");
    statement.execute("rollback");
    rs = statement.executeQuery("select count(*) from test_multi");
    assertTrue(rs.next());
    assertEquals(0, rs.getInt(1));

    statement.close();
    connection.close();
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = RunningOnTravisCI.class)
  public void testMultiStmtLarge() throws SQLException
  {
    // this test verifies that multiple-statement support does not break
    // with many statements
    // it also ensures that results are returned in the correct order
    Connection connection = getConnection();
    enableMultiStmt(connection);
    Statement statement = connection.createStatement();
    StringBuilder multiStmtBuilder = new StringBuilder();
    String query = "SELECT %d;";
    for (int i = 0; i < 100; i++)
    {
      multiStmtBuilder.append(String.format(query, i));
    }

    assertTrue(statement.execute(multiStmtBuilder.toString()));
    for (int i = 0; i < 100; i++)
    {
      ResultSet rs = statement.getResultSet();
      assertNotNull(rs);
      assertEquals(-1, statement.getUpdateCount());
      assertTrue(rs.next());
      assertEquals(i, rs.getInt(1));
      assertFalse(rs.next());

      if (i != 99)
      {
        assertTrue(statement.getMoreResults());
      }
      else
      {
        assertFalse(statement.getMoreResults());
      }

    }

    statement.close();
    connection.close();
  }

}
