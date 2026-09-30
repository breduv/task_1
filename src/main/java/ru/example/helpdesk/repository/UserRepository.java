package ru.example.helpdesk.repository;

import java.util.List;
import java.util.Optional;
import ru.example.helpdesk.model.User;

public interface UserRepository {
    // Создаёт пользователя и заполняет ID, выданный PostgreSQL
    User save(User user);
    // Ищет пользователя по ID
    Optional<User> findById(long id);
    // Ищет пользователя по уникальному адресу электронной почты
    Optional<User> findByEmail(String email);
    // Возвращает всех пользователей
    List<User> findAll();
}
