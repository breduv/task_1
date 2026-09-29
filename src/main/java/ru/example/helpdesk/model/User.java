package ru.example.helpdesk.model;

import java.time.LocalDateTime;

// Общие поля клиента, сотрудника поддержки и администратора.
public abstract class User {
    private long id;
    private String name;
    private String email;
    private Long departmentId;
    private boolean active = true;
    private LocalDateTime createdAt;

    // Создаёт пользователя с основными полями; остальные данные приходят из БД.
    public User(long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Геттеры читают поля пользователя, сеттеры заполняют их после INSERT или SELECT.
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setId(long id) { this.id = id; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Возвращает роль конкретного подкласса для проверки прав и записи в БД.
    public abstract UserRole getRole();

    // Показывает в консоли действие, характерное для конкретной роли.
    public abstract void performAction();
}
