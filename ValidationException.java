package com.example.contactmanagement.exception;

import java.util.List;
import java.util.Arrays;

public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String[] errors;
    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = errors.toArray(String[]::new);
    }
    public List<String> errors() { return List.copyOf(Arrays.asList(errors.clone())); }
}
