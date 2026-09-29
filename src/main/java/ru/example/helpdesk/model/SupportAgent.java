package ru.example.helpdesk.model;

public class SupportAgent extends User {
    /** Создаёт объект сотрудника поддержки из данных таблицы users. */
    public SupportAgent(long id, String name, String email) {
        super(id, name, email);
    }

    /** Выводит учебное действие сотрудника из первой работы. */
    @Override
    public void performAction() {
        System.out.println(getName() + " обрабатывает заявку");
    }

    /** Возвращает роль SUPPORT_AGENT. */
    @Override
    public UserRole getRole() {
        return UserRole.SUPPORT_AGENT;
    }
}
