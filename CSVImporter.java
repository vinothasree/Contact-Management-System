package com.example.contactmanagement.service;

import com.example.contactmanagement.exception.DuplicateContactException;
import com.example.contactmanagement.exception.ValidationException;
import com.example.contactmanagement.model.Contact;
import com.example.contactmanagement.util.CSVUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Imports header-based CSV rows through the same validation and duplicate rules as interactive entry. */
public class CSVImporter {
    public record ImportReport(int imported, List<String> rejected) { public ImportReport { rejected = List.copyOf(rejected); } }
    private final ContactService contacts;
    public CSVImporter(ContactService contacts) { this.contacts = contacts; }

    public ImportReport importFile(Path path) throws IOException {
        List<String> rejected = new ArrayList<>();
        int imported = 0;
        List<List<String>> rows;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { rows = CSVUtil.readAll(reader); }
        if (rows.isEmpty()) return new ImportReport(0, List.of());
        Map<String, Integer> headers = new HashMap<>();
        for (int index = 0; index < rows.get(0).size(); index++) {
            String header = rows.get(0).get(index).trim().toLowerCase(Locale.ROOT);
            if (index == 0) header = header.replace("\uFEFF", "");
            if (headers.putIfAbsent(header, index) != null) throw new IOException("Duplicate CSV header: " + header);
        }
        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (row.size() == 1 && row.get(0).isBlank()) continue;
            try { contacts.add(toContact(row, headers)); imported++; }
            catch (DuplicateContactException | ValidationException | IllegalArgumentException | DateTimeException exception) {
                rejected.add("CSV row " + (index + 1) + ": " + exception.getMessage());
            }
        }
        return new ImportReport(imported, rejected);
    }

    private static Contact toContact(List<String> row, Map<String, Integer> headers) {
        LocalDate birthday = blank(row, headers, "birthday") ? null : LocalDate.parse(get(row, headers, "birthday"));
        boolean favorite = !blank(row, headers, "favorite") && Boolean.parseBoolean(get(row, headers, "favorite"));
        return new Contact(0, get(row,headers,"first_name"), get(row,headers,"last_name"), get(row,headers,"phone"), get(row,headers,"email"), get(row,headers,"address"),
                get(row,headers,"city"), get(row,headers,"state"), get(row,headers,"country"), get(row,headers,"zip_code"), get(row,headers,"company"), get(row,headers,"job_title"),
                birthday, get(row,headers,"notes"), favorite, null, null);
    }
    private static String get(List<String> row, Map<String, Integer> headers, String key) {
        Integer index = headers.get(key);
        return index == null || index >= row.size() || row.get(index).isBlank() ? null : row.get(index).trim();
    }
    private static boolean blank(List<String> row, Map<String, Integer> headers, String key) { return get(row, headers, key) == null; }
}
