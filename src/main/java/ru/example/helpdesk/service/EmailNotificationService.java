package ru.example.helpdesk.service;

public class EmailNotificationService implements NotificationService {
    /** Учебная имитация email: выводит сообщение с пометкой EMAIL. */
    @Override
    public void send(String message) {
        System.out.println("[EMAIL] " + message);
    }
}
