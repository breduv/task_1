package ru.example.helpdesk.service;

public class TelegramNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("[TELEGRAM] " + message);
    }
}