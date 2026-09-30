package ru.example.helpdesk.model;

public class Customer extends User {
    // Создаёт объект клиента
    public Customer(long id, String name, String email) {
        super(id, name, email);
    }

    // Показывает действие клиента в консоли
    @Override
    public void performAction() {
        System.out.println(getName() + " создаёт заявку");
    }

    // Возвращает роль CUSTOMER
    @Override
    public UserRole getRole() {
        return UserRole.CUSTOMER;
    }
}
