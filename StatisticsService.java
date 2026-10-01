package com.example.contactmanagement.service;

import com.example.contactmanagement.dao.ContactDAO;
import com.example.contactmanagement.model.Contact;
import java.util.List;

/** Read-only aggregate dashboard. */
public class StatisticsService {
    public record Dashboard(long totalContacts, long favoriteContacts, long companies,
                            long cities, long countries, List<Contact> recentContacts) {
        public Dashboard { recentContacts = List.copyOf(recentContacts); }
    }
    private final ContactDAO dao;
    public StatisticsService(ContactDAO dao) { this.dao = dao; }
    public Dashboard getDashboard() {
        return new Dashboard(dao.countAll(), dao.countFavorites(), dao.countDistinct("company"),
                dao.countDistinct("city"), dao.countDistinct("country"), dao.recent(5));
    }
}
