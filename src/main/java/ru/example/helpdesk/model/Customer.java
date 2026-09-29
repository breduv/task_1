package ru.example.helpdesk.model;

public class Customer extends User {
    /** Создаёт объект клиента из данных таблицы users. */
    public Customer(long id, String name, String email) {
        super(id, name, email);
    }

    /** Выводит учебное действие клиента из первой работы. */
    @Override
    public void performAction() {
        System.out.println(getName() + " создаёт заявку");
    }

    /** Возвращает роль CUSTOMER. */
    @Override
    public UserRole getRole() {
        return UserRole.CUSTOMER;
    }
}
