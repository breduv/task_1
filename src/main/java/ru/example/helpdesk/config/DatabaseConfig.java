package ru.example.helpdesk.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {

    private static final String URL = System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:postgresql://192.168.1.72:5432/helpdesk_db"
        );

    private static final String USER = System.getenv().getOrDefault("DB_USER", "helpdesk_app");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "change_me_student_password");

    // Не даёт создавать объект класса, содержащего только настройки подключения.
    private DatabaseConfig() {
    }

    // Открывает новое JDBC-соединение; вызывающий код обязан его закрыть.
    public static Connection getConnection() throws SQLException {
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new IllegalStateException("Не задана переменная окружения DB_PASSWORD");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
