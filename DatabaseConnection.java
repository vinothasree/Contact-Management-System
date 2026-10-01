package com.example.contactmanagement.util;

import com.example.contactmanagement.exception.DatabaseException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Creates JDBC connections from environment-based configuration. */
public final class DatabaseConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/contact_management?serverTimezone=UTC";
    private DatabaseConnection() { }

    public static Connection getConnection() {
        String url = env("DB_URL", DEFAULT_URL);
        String user = env("DB_USER", "root");
        String password = env("DB_PASSWORD", "");
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not connect to the contact_management database. Check DB_URL, DB_USER, and DB_PASSWORD. Details: " + exception.getMessage(), exception);
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
