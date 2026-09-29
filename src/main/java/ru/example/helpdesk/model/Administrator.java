package ru.example.helpdesk.model;

public class Administrator extends User {
    // Создаёт объект администратора из данных таблицы users.
    public Administrator(long id, String name, String email) {
        super(id, name, email);
    }

    // Выводит учебное действие администратора из первой работы.
    @Override
    public void performAction() {
        System.out.println(getName() + " управляет системой");
    }

    // Возвращает роль ADMIN.
    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }
}
