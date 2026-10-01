package com.example.contactmanagement.service;

import com.example.contactmanagement.dao.ContactDAO;
import com.example.contactmanagement.exception.ContactNotFoundException;
import com.example.contactmanagement.exception.DuplicateContactException;
import com.example.contactmanagement.model.Contact;
import com.example.contactmanagement.validation.InputValidator;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/** Business rules stay independent of the console and JDBC implementation. */
public class ContactServiceImpl implements ContactService {
    private static final Logger LOGGER = Logger.getLogger(ContactServiceImpl.class.getName());
    private final ContactDAO dao;
    private final Deque<Contact> deletedContacts = new ArrayDeque<>();

    public ContactServiceImpl(ContactDAO dao) { this.dao = dao; }

    @Override public Contact add(Contact contact) {
        InputValidator.validate(contact);
        checkUnique(contact, 0);
        long id = dao.insert(contact);
        LOGGER.info(() -> "Contact created, id=" + id);
        return dao.findById(id).orElseGet(() -> contact.withId(id, null, null));
    }
    @Override public Contact update(Contact contact) {
        if (contact.id() <= 0) throw new IllegalArgumentException("Contact id must be positive");
        InputValidator.validate(contact);
        if (dao.findById(contact.id()).isEmpty()) throw new ContactNotFoundException("Contact " + contact.id() + " not found");
        checkUnique(contact, contact.id());
        if (!dao.update(contact)) throw new ContactNotFoundException("Contact " + contact.id() + " not found");
        LOGGER.info(() -> "Contact updated, id=" + contact.id());
        return dao.findById(contact.id()).orElse(contact);
    }
    @Override public Contact delete(long id) {
        if (id <= 0) throw new IllegalArgumentException("Contact id must be positive");
        Contact deleted = dao.deleteById(id).orElseThrow(() -> new ContactNotFoundException("Contact " + id + " not found"));
        deletedContacts.push(deleted);
        LOGGER.info(() -> "Contact deleted, id=" + id);
        return deleted;
    }
    @Override public Optional<Contact> findById(long id) { return dao.findById(id); }
    @Override public Optional<Contact> findByPhone(String phone) { return dao.findByPhone(phone); }
    @Override public Optional<Contact> findByEmail(String email) { return dao.findByEmail(email); }
    @Override public ContactDAO.Page list(int page, int size, ContactDAO.Sort sort, boolean favorites) { return dao.findAll(page, size, sort, favorites); }
    @Override public List<Contact> search(String query) { return dao.search(query, 100); }
    @Override public List<Contact> search(String field, String term) { return dao.searchByField(field, term, 100); }
    @Override public List<Contact> birthdaysToday() { return dao.birthdaysBetween(LocalDate.now(), LocalDate.now()); }
    @Override public List<Contact> upcomingBirthdays(int days) {
        if (days < 0 || days > 366) throw new IllegalArgumentException("Days must be between 0 and 366");
        LocalDate today = LocalDate.now();
        return dao.birthdaysBetween(today, today.plusDays(days));
    }
    @Override public void favorite(long id, boolean value) {
        if (dao.findById(id).isEmpty()) throw new ContactNotFoundException("Contact " + id + " not found");
        dao.setFavorite(id, value);
    }
    @Override public boolean undoLastDelete() {
        if (deletedContacts.isEmpty()) return false;
        Contact contact = deletedContacts.pop();
        try { add(contact.withId(0, null, null)); return true; }
        catch (RuntimeException exception) { deletedContacts.push(contact); throw exception; }
    }
    private void checkUnique(Contact contact, long id) {
        if (dao.existsByPhone(contact.phone(), id)) throw new DuplicateContactException("Phone number is already in use");
        if (dao.existsByEmail(contact.email(), id)) throw new DuplicateContactException("Email address is already in use");
    }
}
