package ru.example.helpdesk.repository.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ru.example.helpdesk.config.DatabaseConfig;
import ru.example.helpdesk.model.SlaPolicy;
import ru.example.helpdesk.model.TicketPriority;

public class JdbcSlaPolicyRepository {
    // Читает все политики SLA из базы
    public List<SlaPolicy> findAll() {
        String sql = "SELECT * FROM sla_policies ORDER BY resolve_minutes";
        List<SlaPolicy> result = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new SlaPolicy(rs.getLong("id"), rs.getString("name"),
                        TicketPriority.valueOf(rs.getString("priority")),
                        rs.getInt("response_minutes"), rs.getInt("resolve_minutes")));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка чтения политик SLA", e);
        }
    }
}
