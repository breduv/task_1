package ru.example.helpdesk.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.Category;
import ru.example.helpdesk.model.Department;
import ru.example.helpdesk.model.TicketPriority;

public class JdbcCatalogRepository {
    // Загружает все категории для выбора категории новой заявки
    public List<Category> findAllCategories() {
        List<Category> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM categories ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapCategory(rs));
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка чтения категорий", e);
        }
    }

    // Ищет категорию по ID, например для проверки её активности
    public Optional<Category> findCategoryById(long id) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM categories WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapCategory(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка поиска категории", e);
        }
    }

    // Загружает список подразделений службы поддержки
    public List<Department> findAllDepartments() {
        List<Department> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM departments ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapDepartment(rs));
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка чтения подразделений", e);
        }
    }

    // Ищет одно подразделение по ID
    public Optional<Department> findDepartmentById(long id) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM departments WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapDepartment(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка поиска подразделения", e);
        }
    }

    // Преобразует строку categories в объект Category
    private Category mapCategory(ResultSet rs) throws SQLException {
        return new Category(rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                TicketPriority.valueOf(rs.getString("default_priority")), rs.getBoolean("active"));
    }

    // Преобразует строку departments в объект Department
    private Department mapDepartment(ResultSet rs) throws SQLException {
        return new Department(rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
