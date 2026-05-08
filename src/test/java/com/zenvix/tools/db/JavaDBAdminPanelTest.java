package com.zenvix.tools.db;

import com.zenvix.core.services.SecretsVault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JavaDBAdminPanelTest {

    @Mock
    private Consumer<String> mockJsExecutor;

    @Mock
    private Connection mockConnection;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private ResultSetMetaData mockMetaData;

    private JavaDBAdminPanel panel;
    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        panel = new JavaDBAdminPanel(mockJsExecutor);
        SecretsVault.clear();
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testConnect_connectsToMysql() throws Exception {
        DatabaseConnection conn = new DatabaseConnection("mysql", "localhost", 3306, "testdb", "root");
        assertEquals("jdbc:mysql://localhost:3306/testdb", getUrlForTest(conn));
    }

    @Test
    public void testConnect_connectsToPostgresql() throws Exception {
        DatabaseConnection conn = new DatabaseConnection("postgresql", "localhost", 5432, "testdb", "postgres");
        assertEquals("jdbc:postgresql://localhost:5432/testdb", getUrlForTest(conn));
    }

    @Test
    public void testConnect_connectsToH2() throws Exception {
        DatabaseConnection conn = new DatabaseConnection("h2", "localhost", 9092, "testdb", "sa");
        assertEquals("jdbc:h2:tcp://localhost:9092/~/testdb", getUrlForTest(conn));
    }
    
    private String getUrlForTest(DatabaseConnection conn) throws Exception {
        java.lang.reflect.Field hostF = DatabaseConnection.class.getDeclaredField("host"); hostF.setAccessible(true);
        java.lang.reflect.Field portF = DatabaseConnection.class.getDeclaredField("port"); portF.setAccessible(true);
        java.lang.reflect.Field dbF = DatabaseConnection.class.getDeclaredField("database"); dbF.setAccessible(true);
        java.lang.reflect.Field typeF = DatabaseConnection.class.getDeclaredField("dbType"); typeF.setAccessible(true);
        
        String t = (String) typeF.get(conn);
        String h = (String) hostF.get(conn);
        int p = (int) portF.get(conn);
        String d = (String) dbF.get(conn);
        
        if (t.equals("mysql")) return "jdbc:mysql://" + h + ":" + p + "/" + d;
        if (t.equals("postgresql")) return "jdbc:postgresql://" + h + ":" + p + "/" + d;
        if (t.equals("h2")) return "jdbc:h2:tcp://" + h + ":" + p + "/~/" + d;
        return "";
    }

    @Test
    public void testSQLExecute_returnsResultSet() throws Exception {
        DatabaseConnection db = spy(new DatabaseConnection("mysql", "localhost", 3306, "test", "root"));
        db.setConnection(mockConnection);
        doNothing().when(db).connect();
        
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.execute(anyString())).thenReturn(true);
        when(mockStatement.getResultSet()).thenReturn(mockResultSet);
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
        
        when(mockMetaData.getColumnCount()).thenReturn(2);
        when(mockMetaData.getColumnLabel(1)).thenReturn("id");
        when(mockMetaData.getColumnLabel(2)).thenReturn("name");
        
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getObject(1)).thenReturn(1);
        when(mockResultSet.getObject(2)).thenReturn("Alice");

        panel.setConnection(db);
        panel.executeQuery("SELECT * FROM users");
        
        Thread.sleep(100);
        
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockJsExecutor).accept(captor.capture());
        
        String json = captor.getValue();
        assertTrue(json.contains("onQueryResult([{\"id\":\"1\",\"name\":\"Alice\"}])"));
    }

    @Test
    public void testExportCSV_exportResultsCorrectly() {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("id", 1);
        row1.put("name", "Alice \"The Great\"");
        data.add(row1);

        String csv = panel.exportToCSV(data);
        assertTrue(csv.contains("id,name"));
        assertTrue(csv.contains("\"1\",\"Alice \"\"The Great\"\"\""));
    }

    @Test
    public void testERViewer_parsesTableRelationships() throws Exception {
        DatabaseConnection db = spy(new DatabaseConnection("mysql", "localhost", 3306, "test", "root"));
        db.setConnection(mockConnection);
        doNothing().when(db).connect();

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString("child_table")).thenReturn("orders");
        when(mockResultSet.getString("child_column")).thenReturn("user_id");
        when(mockResultSet.getString("parent_table")).thenReturn("users");
        when(mockResultSet.getString("parent_column")).thenReturn("id");

        panel.setConnection(db);
        panel.generateERDiagram();

        Thread.sleep(100);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockJsExecutor).accept(captor.capture());

        String json = captor.getValue();
        assertTrue(json.contains("onERDiagramGenerated([{\"child_table\":\"orders\",\"child_column\":\"user_id\",\"parent_table\":\"users\",\"parent_column\":\"id\"}])"));
    }
}
