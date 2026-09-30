package ru.example.helpdesk.model;

public class Administrator extends User {
    // Создаёт объект администратора
    public Administrator(long id, String name, String email) {
        super(id, name, email);
    }

    // Показывает действие администратора в консоли
    @Override
    public void performAction() {
        System.out.println(getName() + " управляет системой");
    }

    // Возвращает роль ADMIN
    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }
}
