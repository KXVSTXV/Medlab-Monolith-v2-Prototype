-- V2 initially shipped a BCrypt string that did not verify against "Admin@123",
-- so form login failed on MySQL/Flyway while H2 + DevDataLoader (runtime encode) worked.
-- Reset hashes for all Flyway-seeded demo users (plaintext: Admin@123).

UPDATE users
SET password_hash = '$2b$10$3NAqourB0O.3iiKcCVz7lebtnzzpZFp7muXfiK6A8v.Qd0MhwKLFu'
WHERE username IN (
    'admin',
    'labmanager1',
    'labtech1',
    'labtech2',
    'doctor1',
    'reception1',
    'billing1'
)
  AND is_deleted = FALSE;
