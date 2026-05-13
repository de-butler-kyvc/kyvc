ALTER TABLE credentials
    DROP COLUMN IF EXISTS vc_payload_json,
    DROP COLUMN IF EXISTS vc_jwt;
