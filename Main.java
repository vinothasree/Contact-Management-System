package com.example.contactmanagement;

import com.example.contactmanagement.controller.MenuController;
import com.example.contactmanagement.dao.ContactDAO;
import com.example.contactmanagement.dao.impl.ContactDAOImpl;
import com.example.contactmanagement.service.ContactService;
import com.example.contactmanagement.service.ContactServiceImpl;
import com.example.contactmanagement.service.StatisticsService;
import java.util.Scanner;

/** Application composition root. */
public final class Main {
    private Main() { }
    public static void main(String[] args) {
        ContactDAO dao = new ContactDAOImpl();
        ContactService contacts = new ContactServiceImpl(dao);
        new MenuController(new Scanner(System.in), contacts, new StatisticsService(dao)).run();
    }
}
