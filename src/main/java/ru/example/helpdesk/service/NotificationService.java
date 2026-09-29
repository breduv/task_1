package ru.example.helpdesk.service;

public interface NotificationService {
    // Отправляет текст уведомления выбранным способом.
    void send(String message);
}
