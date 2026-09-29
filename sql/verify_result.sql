SELECT * FROM tickets ORDER BY id DESC;

SELECT *
FROM ticket_comments
ORDER BY ticket_id, created_at;

SELECT *
FROM ticket_status_history
ORDER BY ticket_id, changed_at;

SELECT t.id,
       t.title,
       t.status,
       customer.name AS customer,
       assignee.name AS assignee,
       c.name AS category
FROM tickets t
JOIN users customer ON customer.id = t.customer_id
LEFT JOIN users assignee ON assignee.id = t.assignee_id
LEFT JOIN categories c ON c.id = t.category_id
ORDER BY t.id;
