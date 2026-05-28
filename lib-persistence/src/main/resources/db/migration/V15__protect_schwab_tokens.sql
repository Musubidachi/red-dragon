-- AES-GCM/base64 framing expands token values beyond the old varchar(4000)
-- budget. Existing plaintext rows are intentionally left in place because
-- Flyway does not have the local encryption key. The JPA converter can still
-- read them, and new or updated token rows are written encrypted once the key
-- is configured. Historical plaintext audit rows must be re-saved or purged if
-- retroactive cleanup is required.

alter table schwab_token
    alter column access_token type varchar(8192);

alter table schwab_token
    alter column refresh_token type varchar(8192);
