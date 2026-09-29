GRANT USAGE ON SCHEMA public TO helpdesk_app;
GRANT SELECT ON TABLE users, departments, categories, tickets,
    ticket_comments, ticket_status_history TO helpdesk_app;
GRANT INSERT ON TABLE users, tickets, ticket_comments,
    ticket_status_history TO helpdesk_app;
GRANT UPDATE, DELETE ON TABLE tickets TO helpdesk_app;
GRANT USAGE ON SEQUENCE users_id_seq, tickets_id_seq,
    ticket_comments_id_seq, ticket_status_history_id_seq TO helpdesk_app;
