package ru.example.helpdesk.model;

/** Роли пользователей; имена обязаны совпадать с PostgreSQL ENUM user_role. */
public enum UserRole {
    CUSTOMER,
    SUPPORT_AGENT,
    ADMIN
}
