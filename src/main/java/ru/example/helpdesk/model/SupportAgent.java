package ru.example.helpdesk.model;

public class SupportAgent extends User {
    // Создаёт объект сотрудника поддержки
    public SupportAgent(long id, String name, String email) {
        super(id, name, email);
    }

    // Показывает действие сотрудника в консоли
    @Override
    public void performAction() {
        System.out.println(getName() + " обрабатывает заявку");
    }

    // Возвращает роль SUPPORT_AGENT
    @Override
    public UserRole getRole() {
        return UserRole.SUPPORT_AGENT;
    }
}
