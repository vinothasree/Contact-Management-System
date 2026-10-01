package com.example.contactmanagement.dao.impl;

import com.example.contactmanagement.dao.ContactDAO;
import com.example.contactmanagement.exception.DatabaseException;
import com.example.contactmanagement.model.Contact;
import com.example.contactmanagement.util.ConnectionProvider;
import com.example.contactmanagement.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** MySQL implementation; all user values are bound parameters. */
public class ContactDAOImpl implements ContactDAO {
    private static final Logger LOGGER = Logger.getLogger(ContactDAOImpl.class.getName());
    private final ConnectionProvider connections;
    private static final String COLUMNS = "id, first_name, last_name, phone, email, address, city, state, country, zip_code, company, job_title, birthday, notes, favorite, created_at, updated_at";
    private static final String INSERT = "INSERT INTO contacts (first_name,last_name,phone,email,address,city,state,country,zip_code,company,job_title,birthday,notes,favorite) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public ContactDAOImpl() { this(DatabaseConnection::getConnection); }
    public ContactDAOImpl(ConnectionProvider connections) { this.connections = connections; }

    @Override public long insert(Contact c) {
        try (Connection connection = connections.getConnection(); PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, c, 1);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) { if (keys.next()) return keys.getLong(1); }
            throw new SQLException("Insert succeeded but no generated key was returned");
        } catch (SQLException exception) { throw databaseFailure("insert contact", exception); }
    }

    @Override public boolean update(Contact c) {
        String sql = "UPDATE contacts SET first_name=?,last_name=?,phone=?,email=?,address=?,city=?,state=?,country=?,zip_code=?,company=?,job_title=?,birthday=?,notes=?,favorite=?,updated_at=CURRENT_TIMESTAMP WHERE id=?";
        try (Connection connection = connections.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, c, 1); statement.setLong(15, c.id()); return statement.executeUpdate() > 0;
        } catch (SQLException exception) { throw databaseFailure("update contact", exception); }
    }

    @Override public Optional<Contact> deleteById(long id) {
        Optional<Contact> existing = findById(id);
        if (existing.isEmpty()) return existing;
        try (Connection connection = connections.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM contacts WHERE id=?")) {
            statement.setLong(1, id); statement.executeUpdate(); return existing;
        } catch (SQLException exception) { throw databaseFailure("delete contact", exception); }
    }

    @Override public Optional<Contact> findById(long id) { return one("SELECT " + COLUMNS + " FROM contacts WHERE id=?", statement -> statement.setLong(1, id)); }
    @Override public Optional<Contact> findByPhone(String phone) { return one("SELECT " + COLUMNS + " FROM contacts WHERE phone=?", statement -> statement.setString(1, phone)); }
    @Override public Optional<Contact> findByEmail(String email) { return one("SELECT " + COLUMNS + " FROM contacts WHERE email=?", statement -> statement.setString(1, email)); }

    @Override public Page findAll(int page, int pageSize, Sort sort, boolean favoritesOnly) {
        if (page < 0 || pageSize < 1 || pageSize > 500) throw new IllegalArgumentException("Page must be >= 0 and page size must be 1-500");
        String order = switch (sort) {
            case NAME -> "last_name, first_name"; case COMPANY -> "company, last_name"; case CITY -> "city, last_name";
            case NEWEST -> "created_at DESC"; case OLDEST -> "created_at ASC";
        };
        String where = favoritesOnly ? " WHERE favorite=TRUE" : "";
        List<Contact> rows = query("SELECT " + COLUMNS + " FROM contacts" + where + " ORDER BY " + order + " LIMIT ? OFFSET ?", s -> {
            s.setInt(1, pageSize); s.setInt(2, page * pageSize);
        });
        long total = count("SELECT COUNT(*) FROM contacts" + where, null);
        return new Page(rows, page, pageSize, total);
    }

    @Override public List<Contact> search(String query, int limit) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("Search limit must be 1-500");
        String sql = "SELECT " + COLUMNS + " FROM contacts WHERE CONCAT_WS(' ',first_name,last_name,phone,email,company,city,country,address,notes) LIKE ? ORDER BY last_name,first_name LIMIT ?";
        return query(sql, s -> { s.setString(1, "%" + query.trim() + "%"); s.setInt(2, limit); });
    }

    @Override public List<Contact> searchByField(String field, String term, int limit) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("Search limit must be 1-500");
        String column = switch (field.toLowerCase()) {
            case "name" -> "CONCAT_WS(' ',first_name,last_name)";
            case "phone" -> "phone"; case "email" -> "email"; case "company" -> "company";
            case "city" -> "city"; case "country" -> "country"; case "keyword" -> "CONCAT_WS(' ',first_name,last_name,phone,email,company,city,country,address,notes)";
            default -> throw new IllegalArgumentException("Search field must be name, phone, email, company, city, country, or keyword");
        };
        return query("SELECT " + COLUMNS + " FROM contacts WHERE " + column + " LIKE ? ORDER BY last_name,first_name LIMIT ?",
                s -> { s.setString(1, "%" + term.trim() + "%"); s.setInt(2, limit); });
    }

    @Override public List<Contact> birthdaysBetween(LocalDate start, LocalDate end) {
        if (ChronoUnit.DAYS.between(start, end) >= 365)
            return query("SELECT " + COLUMNS + " FROM contacts WHERE birthday IS NOT NULL ORDER BY MONTH(birthday),DAYOFMONTH(birthday),last_name", null);
        int first = start.getMonthValue() * 100 + start.getDayOfMonth();
        int last = end.getMonthValue() * 100 + end.getDayOfMonth();
        String range = first <= last ? "BETWEEN ? AND ?" : ">= ? OR MONTH(birthday)*100+DAYOFMONTH(birthday) <= ?";
        String sql = "SELECT " + COLUMNS + " FROM contacts WHERE birthday IS NOT NULL AND (MONTH(birthday)*100+DAYOFMONTH(birthday) " + range + ") ORDER BY MONTH(birthday),DAYOFMONTH(birthday),last_name";
        return query(sql, s -> { s.setInt(1, first); s.setInt(2, last); });
    }
    @Override public boolean existsByPhone(String phone, long excludingId) { return exists("phone", phone, excludingId); }
    @Override public boolean existsByEmail(String email, long excludingId) { return exists("email", email, excludingId); }
    private boolean exists(String field, String value, long id) {
        if (!field.equals("phone") && !field.equals("email")) throw new IllegalArgumentException("Unsupported field");
        return count("SELECT COUNT(*) FROM contacts WHERE " + field + "=? AND id<>?", s -> { s.setString(1, value); s.setLong(2, id); }) > 0;
    }
    @Override public void setFavorite(long id, boolean favorite) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement("UPDATE contacts SET favorite=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
            s.setBoolean(1, favorite); s.setLong(2, id); if (s.executeUpdate() == 0) throw new com.example.contactmanagement.exception.ContactNotFoundException("Contact " + id + " not found");
        } catch (SQLException e) { throw databaseFailure("change favorite", e); }
    }
    @Override public long countAll() { return count("SELECT COUNT(*) FROM contacts", null); }
    @Override public long countFavorites() { return count("SELECT COUNT(*) FROM contacts WHERE favorite=TRUE", null); }
    @Override public long countDistinct(String column) {
        if (!List.of("company", "city", "country").contains(column)) throw new IllegalArgumentException("Unsupported statistics column");
        return count("SELECT COUNT(DISTINCT NULLIF(TRIM(" + column + "),'')) FROM contacts", null);
    }
    @Override public List<Contact> recent(int limit) { return query("SELECT " + COLUMNS + " FROM contacts ORDER BY created_at DESC LIMIT ?", s -> s.setInt(1, Math.min(Math.max(limit, 1), 100))); }

    private Optional<Contact> one(String sql, Binder binder) {
        List<Contact> rows = query(sql, binder); return rows.stream().findFirst();
    }
    private List<Contact> query(String sql, Binder binder) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            if (binder != null) binder.bind(s);
            try (ResultSet r = s.executeQuery()) { List<Contact> rows = new ArrayList<>(); while (r.next()) rows.add(map(r)); return rows; }
        } catch (SQLException e) { throw databaseFailure("read contacts", e); }
    }
    private long count(String sql, Binder binder) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            if (binder != null) binder.bind(s);
            try (ResultSet r = s.executeQuery()) { return r.next() ? r.getLong(1) : 0; }
        } catch (SQLException e) { throw databaseFailure("read statistics", e); }
    }
    private static Contact map(ResultSet r) throws SQLException {
        java.sql.Date birthday = r.getDate("birthday");
        Timestamp created = r.getTimestamp("created_at"), updated = r.getTimestamp("updated_at");
        return new Contact(r.getLong("id"), r.getString("first_name"), r.getString("last_name"), r.getString("phone"), r.getString("email"),
                r.getString("address"), r.getString("city"), r.getString("state"), r.getString("country"), r.getString("zip_code"), r.getString("company"), r.getString("job_title"),
                birthday == null ? null : birthday.toLocalDate(), r.getString("notes"), r.getBoolean("favorite"),
                created == null ? null : created.toLocalDateTime(), updated == null ? null : updated.toLocalDateTime());
    }
    private static void bind(PreparedStatement s, Contact c, int start) throws SQLException {
        s.setString(start, c.firstName()); s.setString(start + 1, c.lastName()); s.setString(start + 2, c.phone()); s.setString(start + 3, c.email());
        s.setString(start + 4, c.address()); s.setString(start + 5, c.city()); s.setString(start + 6, c.state()); s.setString(start + 7, c.country());
        s.setString(start + 8, c.zipCode()); s.setString(start + 9, c.company()); s.setString(start + 10, c.jobTitle());
        if (c.birthday() == null) s.setNull(start + 11, Types.DATE); else s.setDate(start + 11, java.sql.Date.valueOf(c.birthday()));
        s.setString(start + 12, c.notes()); s.setBoolean(start + 13, c.favorite());
    }
    private static RuntimeException databaseFailure(String action, SQLException e) {
        LOGGER.log(Level.SEVERE, "Failed to " + action, e);
        if (e.getErrorCode() == 1062) return new com.example.contactmanagement.exception.DuplicateContactException("Phone number or email address is already in use");
        return new DatabaseException("Unable to " + action + ": " + e.getMessage(), e);
    }
    @FunctionalInterface private interface Binder { void bind(PreparedStatement statement) throws SQLException; }
}
