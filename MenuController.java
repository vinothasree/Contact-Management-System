package com.example.contactmanagement.controller;

import com.example.contactmanagement.dao.ContactDAO;
import com.example.contactmanagement.exception.ContactNotFoundException;
import com.example.contactmanagement.model.Contact;
import com.example.contactmanagement.service.BackupService;
import com.example.contactmanagement.service.CSVExporter;
import com.example.contactmanagement.service.CSVImporter;
import com.example.contactmanagement.service.ContactService;
import com.example.contactmanagement.service.JSONExporter;
import com.example.contactmanagement.service.StatisticsService;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/** Console MVC controller; prompts and presentation are kept out of the service/DAO layers. */
public class MenuController {
    private final Scanner input;
    private final ContactService contacts;
    private final StatisticsService statistics;
    private final CSVExporter csvExporter = new CSVExporter();
    private final JSONExporter jsonExporter = new JSONExporter();
    private final BackupService backupService = new BackupService();

    public MenuController(Scanner input, ContactService contacts, StatisticsService statistics) {
        this.input = input; this.contacts = contacts; this.statistics = statistics;
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Choose an option");
            try {
                switch (choice) {
                    case "1" -> addContact(); case "2" -> viewContacts(); case "3" -> searchContacts();
                    case "4" -> updateContact(); case "5" -> deleteContact(); case "6" -> favoritesMenu();
                    case "7" -> importContacts(); case "8" -> exportContacts(); case "9" -> showStatistics();
                    case "10" -> backup(); case "11" -> restore(); case "12" -> running = false;
                    default -> System.out.println("Choose a number from 1 to 12.");
                }
            } catch (RuntimeException | IOException exception) {
                System.out.println("Operation failed: " + exception.getMessage());
            }
        }
        System.out.println("Goodbye.");
    }

    private void printMenu() {
        System.out.println("\n============================\nCONTACT MANAGEMENT SYSTEM\n============================");
        System.out.println("1. Add Contact\n2. View Contacts\n3. Search Contact\n4. Update Contact\n5. Delete Contact\n6. Favorites\n7. Import Contacts\n8. Export Contacts\n9. Statistics\n10. Backup\n11. Restore\n12. Exit");
    }

    private void addContact() {
        Contact candidate = readContact(null);
        Contact added = contacts.add(candidate);
        System.out.println("Added contact with ID " + added.id());
    }

    private void viewContacts() {
        System.out.println("1. All contacts  2. By ID  3. By phone  4. By email  5. Today's birthdays  6. Upcoming birthdays");
        switch (prompt("View option")) {
            case "2" -> contacts.findById(readLong("Contact ID")).ifPresentOrElse(this::printContact, () -> System.out.println("Contact not found."));
            case "3" -> contacts.findByPhone(prompt("Phone")).ifPresentOrElse(this::printContact, () -> System.out.println("Contact not found."));
            case "4" -> contacts.findByEmail(prompt("Email")).ifPresentOrElse(this::printContact, () -> System.out.println("Contact not found."));
            case "5" -> printTable(contacts.birthdaysToday());
            case "6" -> printTable(contacts.upcomingBirthdays(readInt("Days ahead (1-366)")));
            case "1" -> listContacts(false);
            default -> System.out.println("Invalid view option.");
        }
    }

    private void listContacts(boolean favoritesOnly) {
        ContactDAO.Sort sort = selectSort();
        int page = Math.max(0, readInt("Page number (starting at 1)") - 1);
        ContactDAO.Page result = contacts.list(page, 20, sort, favoritesOnly);
        printTable(result.contacts());
        System.out.printf(Locale.ROOT, "Page %d of %d (%d contacts)%n", result.page() + 1, result.pageCount(), result.total());
    }

    private ContactDAO.Sort selectSort() {
        System.out.println("Sort: 1 Name  2 Company  3 City  4 Newest  5 Oldest");
        return switch (prompt("Sort option")) {
            case "2" -> ContactDAO.Sort.COMPANY; case "3" -> ContactDAO.Sort.CITY;
            case "4" -> ContactDAO.Sort.NEWEST; case "5" -> ContactDAO.Sort.OLDEST; default -> ContactDAO.Sort.NAME;
        };
    }

    private void searchContacts() {
        System.out.println("Search field: 1 Name  2 Phone  3 Email  4 Company  5 City  6 Country  7 Keyword");
        String field = switch (prompt("Search field")) {
            case "1" -> "name"; case "2" -> "phone"; case "3" -> "email"; case "4" -> "company";
            case "5" -> "city"; case "6" -> "country"; case "7" -> "keyword";
            default -> throw new IllegalArgumentException("Choose a search field from 1 to 7");
        };
        printTable(contacts.search(field, prompt("Search term")));
    }

    private void updateContact() {
        long id = readLong("Contact ID");
        Contact old = contacts.findById(id).orElseThrow(() -> new ContactNotFoundException("Contact " + id + " not found"));
        Contact changed = readContact(old);
        contacts.update(changed);
        System.out.println("Contact updated.");
    }

    private Contact readContact(Contact old) {
        return new Contact(old == null ? 0 : old.id(), value("First name", old == null ? null : old.firstName()),
                value("Last name", old == null ? null : old.lastName()), value("Phone", old == null ? null : old.phone()),
                value("Email", old == null ? null : old.email()), value("Address", old == null ? null : old.address()),
                value("City", old == null ? null : old.city()), value("State", old == null ? null : old.state()),
                value("Country", old == null ? null : old.country()), value("Zip code", old == null ? null : old.zipCode()),
                value("Company", old == null ? null : old.company()), value("Job title", old == null ? null : old.jobTitle()),
                dateValue(old), value("Notes", old == null ? null : old.notes()), old != null && old.favorite(),
                old == null ? null : old.createdAt(), old == null ? null : old.updatedAt());
    }

    private String value(String label, String current) {
        String hint = current == null ? "" : " [" + current + "]";
        String value = prompt(label + hint + (current == null ? " (optional: - to leave blank)" : " (Enter keeps current)"));
        if (value.isEmpty() && current != null) return current;
        if (value.equals("-") || value.isEmpty()) return null;
        return value;
    }

    private LocalDate dateValue(Contact old) {
        String hint = old == null || old.birthday() == null ? "" : " [" + old.birthday() + "]";
        String value = prompt("Birthday YYYY-MM-DD" + hint + " (optional; Enter keeps current)");
        if (value.isEmpty()) return old == null ? null : old.birthday();
        if (value.equals("-")) return null;
        return LocalDate.parse(value);
    }

    private void deleteContact() {
        long id = readLong("Contact ID");
        Contact found = contacts.findById(id).orElseThrow(() -> new ContactNotFoundException("Contact " + id + " not found"));
        System.out.println("Delete " + found.fullName().trim() + "? This cannot be automatically rolled back.");
        if (prompt("Type YES to confirm").equalsIgnoreCase("YES")) {
            contacts.delete(id);
            System.out.println("Deleted. Choose Favorites > Undo last delete to restore the last entry.");
        } else System.out.println("Deletion cancelled.");
    }

    private void favoritesMenu() {
        System.out.println("1. List favorites  2. Mark favorite  3. Remove favorite  4. Undo last delete");
        switch (prompt("Favorites option")) {
            case "1" -> listContacts(true);
            case "2" -> { contacts.favorite(readLong("Contact ID"), true); System.out.println("Marked favorite."); }
            case "3" -> { contacts.favorite(readLong("Contact ID"), false); System.out.println("Favorite removed."); }
            case "4" -> System.out.println(contacts.undoLastDelete() ? "Last deleted contact restored." : "Nothing to undo.");
            default -> System.out.println("Invalid favorites option.");
        }
    }

    private void importContacts() throws IOException {
        CSVImporter.ImportReport report = new CSVImporter(contacts).importFile(Path.of(prompt("CSV file path")));
        System.out.println("Imported " + report.imported() + " contacts; rejected " + report.rejected().size() + ".");
        report.rejected().forEach(System.out::println);
    }

    private void exportContacts() throws IOException {
        List<Contact> all = allContacts();
        String format = prompt("Format: CSV, TXT, or JSON").toLowerCase(Locale.ROOT);
        Path path = Path.of(prompt("Output file path"));
        switch (format) {
            case "csv" -> csvExporter.export(all, path); case "txt" -> csvExporter.exportText(all, path);
            case "json" -> jsonExporter.export(all, path); default -> throw new IllegalArgumentException("Format must be CSV, TXT, or JSON");
        }
        System.out.println("Exported " + all.size() + " contacts to " + path.toAbsolutePath());
    }

    private List<Contact> allContacts() {
        List<Contact> all = new ArrayList<>(); int page = 0; ContactDAO.Page result;
        do { result = contacts.list(page++, 500, ContactDAO.Sort.NAME, false); all.addAll(result.contacts()); } while (all.size() < result.total());
        return all;
    }

    private void showStatistics() {
        StatisticsService.Dashboard d = statistics.getDashboard();
        System.out.printf("Contacts: %d | Favorites: %d | Companies: %d | Cities: %d | Countries: %d%n", d.totalContacts(), d.favoriteContacts(), d.companies(), d.cities(), d.countries());
        System.out.println("Recent contacts:"); printTable(d.recentContacts());
    }
    private void backup() throws IOException { Path path = Path.of(prompt("SQL backup output path")); backupService.exportSql(path); System.out.println("Backup written to " + path.toAbsolutePath()); }
    private void restore() throws IOException {
        Path path = Path.of(prompt("Trusted SQL backup file path"));
        if (!prompt("Restore contacts from this file? Type YES to confirm").equalsIgnoreCase("YES")) { System.out.println("Restore cancelled."); return; }
        System.out.println("Executed " + backupService.restoreSql(path) + " restore statements.");
    }

    private void printContact(Contact c) { printTable(List.of(c)); }
    private void printTable(List<Contact> list) {
        if (list.isEmpty()) { System.out.println("No contacts found."); return; }
        System.out.printf("%-5s %-24s %-18s %-30s %-22s %-6s%n", "ID", "NAME", "PHONE", "EMAIL", "COMPANY", "FAV");
        System.out.println("-".repeat(111));
        for (Contact c : list) System.out.printf(Locale.ROOT, "%-5d %-24s %-18s %-30s %-22s %-6s%n", c.id(), clip(c.fullName().trim(),24), clip(c.phone(),18), clip(c.email(),30), clip(c.company(),22), c.favorite() ? "*" : "");
    }
    private static String clip(String value, int max) { if (value == null) return ""; return value.length() <= max ? value : value.substring(0, max - 1) + "..."; }
    private String prompt(String label) { System.out.print(label + ": "); return input.nextLine().trim(); }
    private long readLong(String label) { try { return Long.parseLong(prompt(label)); } catch (NumberFormatException e) { throw new IllegalArgumentException(label + " must be a whole number"); } }
    private int readInt(String label) { try { return Integer.parseInt(prompt(label)); } catch (NumberFormatException e) { throw new IllegalArgumentException(label + " must be a whole number"); } }
}
