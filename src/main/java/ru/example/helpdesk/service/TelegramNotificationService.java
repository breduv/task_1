package ru.example.helpdesk.service;

public class TelegramNotificationService implements NotificationService {
    // Пока просто печатает сообщение с пометкой TELEGRAM
    @Override
    public void send(String message) {
        System.out.println("[TELEGRAM] " + message);
    }
}
