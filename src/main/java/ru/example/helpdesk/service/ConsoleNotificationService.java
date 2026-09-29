package ru.example.helpdesk.service;

public class ConsoleNotificationService implements NotificationService {
    /** Печатает уведомление в консоли; этот вариант используется в Main. */
    @Override
    public void send(String message) {
        System.out.println("[УВЕДОМЛЕНИЕ] " + message);
    }
}
