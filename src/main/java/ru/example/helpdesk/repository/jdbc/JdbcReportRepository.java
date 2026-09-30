package ru.example.helpdesk.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.AgentTicketStats;

public class JdbcReportRepository {
    // Считает заявки в каждом статусе
    public Map<String, Long> countByStatus() {
        return count("SELECT status::text AS label, COUNT(*) AS count FROM tickets GROUP BY status ORDER BY status");
    }

    // Считает заявки в каждой категории, включая категории без заявок
    public Map<String, Long> countByCategory() {
        return count("""
                SELECT c.name AS label, COUNT(t.id) AS count
                FROM categories c LEFT JOIN tickets t ON t.category_id = c.id
                GROUP BY c.id, c.name ORDER BY count DESC
                """);
    }

    // Показывает число активных заявок у каждого сотрудника поддержки
    public Map<String, Long> activeByAgent() {
        return count("""
                SELECT u.name AS label,
                       COUNT(t.id) FILTER (WHERE t.status IN ('NEW', 'IN_PROGRESS')) AS count
                FROM users u LEFT JOIN tickets t ON t.assignee_id = u.id
                WHERE u.role = 'SUPPORT_AGENT'
                GROUP BY u.id, u.name ORDER BY count DESC
                """);
    }

    // Одним запросом считает активные, решённые и закрытые заявки сотрудников
    public List<AgentTicketStats> ticketCountsByAgent() {
        String sql = """
                SELECT u.name,
                       COUNT(t.id) FILTER (WHERE t.status IN ('NEW', 'IN_PROGRESS')) AS active,
                       COUNT(t.id) FILTER (WHERE t.status = 'RESOLVED') AS resolved,
                       COUNT(t.id) FILTER (WHERE t.status = 'CLOSED') AS closed
                FROM users u LEFT JOIN tickets t ON t.assignee_id = u.id
                WHERE u.role = 'SUPPORT_AGENT'
                GROUP BY u.id, u.name ORDER BY u.name
                """;
        List<AgentTicketStats> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new AgentTicketStats(rs.getString("name"), rs.getLong("active"),
                        rs.getLong("resolved"), rs.getLong("closed")));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка подсчёта заявок сотрудников", e);
        }
    }

    // Считает среднее время решения закрытых заявок в минутах
    public OptionalDouble averageResolutionMinutes() {
        String sql = """
                SELECT AVG(EXTRACT(EPOCH FROM (closed_at - created_at)) / 60.0) AS minutes
                FROM tickets WHERE status = 'CLOSED' AND closed_at IS NOT NULL
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            double minutes = rs.getDouble("minutes");
            return rs.wasNull() ? OptionalDouble.empty() : OptionalDouble.of(minutes);
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка расчёта среднего времени решения", e);
        }
    }

    // Считает среднее время решения отдельно для каждой категории
    public Map<String, Double> averageResolutionMinutesByCategory() {
        String sql = """
                SELECT COALESCE(c.name, 'Без категории') AS label,
                       AVG(EXTRACT(EPOCH FROM (t.closed_at - t.created_at)) / 60.0) AS minutes
                FROM tickets t LEFT JOIN categories c ON c.id = t.category_id
                WHERE t.status = 'CLOSED' AND t.closed_at IS NOT NULL
                GROUP BY c.id, c.name ORDER BY c.name
                """;
        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.put(rs.getString("label"), rs.getDouble("minutes"));
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка расчёта времени по категориям", e);
        }
    }

    // Собирает результаты отчёта в пары «название — количество»
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
