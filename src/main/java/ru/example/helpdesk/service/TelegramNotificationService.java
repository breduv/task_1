package ru.example.helpdesk.service;

public class TelegramNotificationService implements NotificationService {
    // Учебная имитация Telegram: выводит сообщение с пометкой TELEGRAM.
    @Override
    public void send(String message) {
        System.out.println("[TELEGRAM] " + message);
    }
}
