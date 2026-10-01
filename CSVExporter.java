package com.example.contactmanagement.service;

import com.example.contactmanagement.model.Contact;
import com.example.contactmanagement.util.CSVUtil;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class CSVExporter {
    public void export(List<Contact> contacts, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            CSVUtil.writeRecord(writer, List.of("id","first_name","last_name","phone","email","address","city","state","country","zip_code","company","job_title","birthday","notes","favorite"));
            for (Contact c : contacts) CSVUtil.writeRecord(writer, Arrays.asList(String.valueOf(c.id()), c.firstName(), c.lastName(), c.phone(), c.email(), c.address(), c.city(), c.state(), c.country(), c.zipCode(), c.company(), c.jobTitle(), c.birthday() == null ? null : c.birthday().toString(), c.notes(), String.valueOf(c.favorite())));
        }
    }
    public void exportText(List<Contact> contacts, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Contact c : contacts) writer.write(c.id() + " | " + c.fullName().trim() + " | " + c.phone() + " | " + c.email() + System.lineSeparator());
        }
    }
}
