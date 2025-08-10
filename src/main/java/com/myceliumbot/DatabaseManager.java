package com.myceliumbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private String dbPath;
    private String dbUrl; // Add this field

    // Default constructor - uses config
    public DatabaseManager() {
        this.dbPath = BotConfig.getInstance().getDatabasePath();
        this.dbUrl = "jdbc:sqlite:" + dbPath; // Initialize dbUrl

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            initializeDatabase(conn); // Call initializeDatabase
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    private void initializeDatabase(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mod_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "guild_id TEXT NOT NULL," +
                "moderator_id TEXT NOT NULL," +
                "target_id TEXT NOT NULL," +
                "action TEXT NOT NULL," +
                "reason TEXT," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ");";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Initialized 'mod_logs' table.");
        }
    }

    public void execute(String sql, Object... params) {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB execute error: " + e.getMessage());
        }
    }

    public String query(String sql, Object... params) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode results = mapper.createArrayNode();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData md = rs.getMetaData();
            int columns = md.getColumnCount();

            while (rs.next()) {
                ObjectNode row = mapper.createObjectNode();
                for (int i = 1; i <= columns; i++) {
                    row.put(md.getColumnName(i), rs.getString(i));
                }
                results.add(row);
            }
        } catch (SQLException e) {
            System.err.println("DB query error: " + e.getMessage());
            return "[]"; // Return empty JSON array on error
        }
        return results.toString();
    }
}
