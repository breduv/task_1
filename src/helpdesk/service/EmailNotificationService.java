package helpdesk.service;

public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("[EMAIL] " + message);
    }
}