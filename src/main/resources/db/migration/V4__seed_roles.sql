-- Seed the two roles the application currently references so a fresh
-- database can register users without a manual bootstrap step.
--
-- INSERT IGNORE keeps the migration idempotent-in-effect for existing
-- databases that already have one or both rows (harmless if the id
-- differs; addRoleToUser looks up by name, not id).

INSERT IGNORE INTO roles (name) VALUES ('ROLE_USER');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN');
