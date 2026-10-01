package com.example.contactmanagement.validation;

import com.example.contactmanagement.exception.ValidationException;
import com.example.contactmanagement.model.Contact;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Central validation rules for user-supplied contact data. */
public final class InputValidator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^[+()0-9 .-]{7,20}$");
    private static final Pattern ZIP = Pattern.compile("^[\\p{L}0-9 -]{1,20}$");
    private static final Pattern NAME = Pattern.compile("(?=.*\\p{L})[\\p{L}\\p{M} .'-]+");
    private InputValidator() { }

    public static List<String> errors(Contact contact) {
        List<String> errors = new ArrayList<>();
        if (contact == null) return List.of("Contact is required");
        required(contact.firstName(), "First name", 50, errors);
        required(contact.lastName(), "Last name", 50, errors);
        name(contact.firstName(), "First name", errors);
        name(contact.lastName(), "Last name", errors);
        if (contact.phone() == null || !PHONE.matcher(contact.phone().trim()).matches() || contact.phone().replaceAll("\\D", "").length() < 7)
            errors.add("Phone must contain 7-20 digits and may include +, spaces, parentheses, dots, or hyphens");
        if (contact.email() == null || contact.email().length() > 100 || !EMAIL.matcher(contact.email().trim()).matches())
            errors.add("Email address is invalid or exceeds 100 characters");
        max(contact.address(), "Address", 255, errors);
        max(contact.city(), "City", 50, errors);
        max(contact.state(), "State", 50, errors);
        max(contact.country(), "Country", 50, errors);
        if (contact.zipCode() != null && !contact.zipCode().isBlank() && !ZIP.matcher(contact.zipCode().trim()).matches()) errors.add("Zip code is invalid");
        max(contact.company(), "Company", 100, errors);
        max(contact.jobTitle(), "Job title", 100, errors);
        if (contact.birthday() != null && contact.birthday().isAfter(java.time.LocalDate.now())) errors.add("Birthday cannot be in the future");
        return List.copyOf(errors);
    }

    public static void validate(Contact contact) {
        List<String> errors = errors(contact);
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }

    private static void required(String value, String field, int limit, List<String> errors) {
        if (value == null || value.isBlank()) errors.add(field + " is required");
        else if (value.trim().length() > limit) errors.add(field + " must be at most " + limit + " characters");
    }
    private static void max(String value, String field, int limit, List<String> errors) {
        if (value != null && value.length() > limit) errors.add(field + " must be at most " + limit + " characters");
    }
    private static void name(String value, String field, List<String> errors) {
        if (value != null && !value.isBlank() && !NAME.matcher(value.trim()).matches()) errors.add(field + " contains invalid characters");
    }
}
