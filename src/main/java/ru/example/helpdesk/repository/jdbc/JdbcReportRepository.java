package ru.example.helpdesk.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.example.helpdesk.config.DatabaseConfig;

public class JdbcReportRepository {
    // Считает заявки в каждом статусе.
    public Map<String, Long> countByStatus() {
        return count("SELECT status::text AS label, COUNT(*) AS count FROM tickets GROUP BY status ORDER BY status");
    }

    // Считает заявки в каждой категории, включая категории без заявок.
    public Map<String, Long> countByCategory() {
        return count("""
                SELECT c.name AS label, COUNT(t.id) AS count
                FROM categories c LEFT JOIN tickets t ON t.category_id = c.id
                GROUP BY c.id, c.name ORDER BY count DESC
                """);
    }

    // Показывает число активных заявок у каждого сотрудника поддержки.
    public Map<String, Long> activeByAgent() {
        return count("""
                SELECT u.name AS label,
                       COUNT(t.id) FILTER (WHERE t.status IN ('NEW', 'IN_PROGRESS')) AS count
                FROM users u LEFT JOIN tickets t ON t.assignee_id = u.id
                WHERE u.role = 'SUPPORT_AGENT'
                GROUP BY u.id, u.name ORDER BY count DESC
                """);
    }

    // Выполняет отчётный SELECT и собирает пары «название — количество».
    private Map<String, Long> count(String sql) {
        Map<String, Long> result = new LinkedHashMap<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.put(rs.getString("label"), rs.getLong("count"));
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка построения отчёта", e);
        }
    }
}
