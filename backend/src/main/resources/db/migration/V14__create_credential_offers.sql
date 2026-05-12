CREATE TABLE credential_offers (
    credential_offer_id BIGSERIAL PRIMARY KEY,
    kyc_id BIGINT NOT NULL,
    corporate_id BIGINT NOT NULL,
    offer_token_hash VARCHAR(255) NOT NULL,
    offer_status_code VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    credential_id BIGINT NULL,
    device_id VARCHAR(255) NULL,
    holder_did VARCHAR(255) NULL,
    holder_xrpl_address VARCHAR(255) NULL,
    failure_reason_code VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_credential_offers_kyc FOREIGN KEY (kyc_id) REFERENCES kyc_applications (kyc_id),
    CONSTRAINT fk_credential_offers_corporate FOREIGN KEY (corporate_id) REFERENCES corporates (corporate_id),
    CONSTRAINT fk_credential_offers_credential FOREIGN KEY (credential_id) REFERENCES credentials (credential_id)
);

CREATE INDEX idx_credential_offers_kyc_id
    ON credential_offers (kyc_id);

CREATE INDEX idx_credential_offers_corporate_id
    ON credential_offers (corporate_id);

CREATE INDEX idx_credential_offers_status_expires_at
    ON credential_offers (offer_status_code, expires_at);

CREATE INDEX idx_credential_offers_credential_id
    ON credential_offers (credential_id);

CREATE INDEX idx_credential_offers_corporate_status
    ON credential_offers (corporate_id, offer_status_code);
