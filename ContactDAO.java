package com.example.contactmanagement.dao;

import com.example.contactmanagement.model.Contact;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Persistence contract for contacts. */
public interface ContactDAO {
    enum Sort { NAME, COMPANY, CITY, NEWEST, OLDEST }
    record Page(List<Contact> contacts, int page, int pageSize, long total) {
        public Page { contacts = List.copyOf(contacts); }
        public int pageCount() { return pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize); }
    }
    long insert(Contact contact);
    boolean update(Contact contact);
    Optional<Contact> deleteById(long id);
    Optional<Contact> findById(long id);
    Optional<Contact> findByPhone(String phone);
    Optional<Contact> findByEmail(String email);
    Page findAll(int page, int pageSize, Sort sort, boolean favoritesOnly);
    List<Contact> search(String query, int limit);
    List<Contact> searchByField(String field, String term, int limit);
    List<Contact> birthdaysBetween(LocalDate start, LocalDate end);
    boolean existsByPhone(String phone, long excludingId);
    boolean existsByEmail(String email, long excludingId);
    void setFavorite(long id, boolean favorite);
    long countAll();
    long countFavorites();
    long countDistinct(String column);
    List<Contact> recent(int limit);
}
