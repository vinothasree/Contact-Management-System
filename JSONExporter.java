package com.example.contactmanagement.service;

import com.example.contactmanagement.model.Contact;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class JSONExporter {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT);
    public void export(List<Contact> contacts, Path path) throws IOException { mapper.writeValue(path.toFile(), contacts); }
}
