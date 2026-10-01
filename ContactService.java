package com.example.contactmanagement.service;

import com.example.contactmanagement.dao.ContactDAO;
import com.example.contactmanagement.model.Contact;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContactService {
    Contact add(Contact contact);
    Contact update(Contact contact);
    Contact delete(long id);
    Optional<Contact> findById(long id);
    Optional<Contact> findByPhone(String phone);
    Optional<Contact> findByEmail(String email);
    ContactDAO.Page list(int page, int pageSize, ContactDAO.Sort sort, boolean favoritesOnly);
    List<Contact> search(String query);
    List<Contact> search(String field, String term);
    List<Contact> birthdaysToday();
    List<Contact> upcomingBirthdays(int days);
    void favorite(long id, boolean value);
    boolean undoLastDelete();
}
