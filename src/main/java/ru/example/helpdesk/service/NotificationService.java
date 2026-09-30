package ru.example.helpdesk.service;

public interface NotificationService {
    // Передаёт сообщение выбранному способу уведомления
    void send(String message);
}
