package com.example.contactmanagement.service;

import com.example.contactmanagement.exception.DatabaseException;
import com.example.contactmanagement.util.DatabaseConnection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Exports and restores the contacts table as SQL. Restore scripts must be trusted local files. */
public class BackupService {
    private static final String SELECT = "SELECT first_name,last_name,phone,email,address,city,state,country,zip_code,company,job_title,birthday,notes,favorite FROM contacts";

    public void exportSql(Path path) throws IOException {
        StringBuilder dump = new StringBuilder("-- Contact Management System data backup\nUSE contact_management;\n");
        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(SELECT)) {
            dump.append("DELETE FROM contacts;\n");
            while (rows.next()) {
                List<String> values = new ArrayList<>();
                for (int i = 1; i <= 14; i++) {
                    Object value = rows.getObject(i);
                    if (value == null) values.add("NULL");
                    else if (value instanceof Boolean bool) values.add(bool ? "TRUE" : "FALSE");
                    else values.add("'" + value.toString().replace("\\", "\\\\").replace("'", "''") + "'");
                }
                dump.append("INSERT INTO contacts (first_name,last_name,phone,email,address,city,state,country,zip_code,company,job_title,birthday,notes,favorite) VALUES (")
                        .append(String.join(",", values)).append(");\n");
            }
        } catch (SQLException exception) { throw new DatabaseException("Unable to export SQL backup", exception); }
        Files.writeString(path, dump.toString());
    }

    public int restoreSql(Path path) throws IOException {
        String sql = Files.readString(path);
        if (!sql.toLowerCase().contains("insert into contacts"))
            throw new IllegalArgumentException("Restore file must contain contact INSERT statements");
        List<String> statements = splitStatements(sql);
        for (String item : statements) {
            String command = item.trim().toLowerCase();
            if (!command.isEmpty() && !command.startsWith("--") && !command.startsWith("use contact_management")
                    && !command.startsWith("delete from contacts") && !command.startsWith("insert into contacts"))
                throw new IllegalArgumentException("Restore file contains a statement that is not allowed");
        }
        int executed = 0;
        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                for (String item : statements) {
                    String command = item.trim();
                    if (command.isEmpty() || command.startsWith("--") || command.regionMatches(true, 0, "USE ", 0, 4)) continue;
                    statement.execute(command);
                    executed++;
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally { connection.setAutoCommit(previousAutoCommit); }
        } catch (SQLException exception) { throw new DatabaseException("Unable to restore SQL backup", exception); }
        return executed;
    }

    private static List<String> splitStatements(String sql) {
        List<String> result = new ArrayList<>(); StringBuilder current = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'' && (i == 0 || sql.charAt(i - 1) != '\\')) quoted = !quoted;
            if (ch == ';' && !quoted) { result.add(current.toString()); current.setLength(0); } else current.append(ch);
        }
        if (!current.isEmpty()) result.add(current.toString());
        return result;
    }
}
