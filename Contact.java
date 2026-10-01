package com.example.contactmanagement.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Immutable contact entity. */
public record Contact(long id, String firstName, String lastName, String phone, String email,
                      String address, String city, String state, String country, String zipCode,
                      String company, String jobTitle, LocalDate birthday, String notes,
                      boolean favorite, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public String fullName() {
        return (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName).trim();
    }

    public Contact withId(long newId, LocalDateTime created, LocalDateTime updated) {
        return new Contact(newId, firstName, lastName, phone, email, address, city, state, country,
                zipCode, company, jobTitle, birthday, notes, favorite, created, updated);
    }

    public Contact withFavorite(boolean value) {
        return new Contact(id, firstName, lastName, phone, email, address, city, state, country,
                zipCode, company, jobTitle, birthday, notes, value, createdAt, updatedAt);
    }
}
