package com.example.contactmanagement.exception;

public class DuplicateContactException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public DuplicateContactException(String message) { super(message); }
}
