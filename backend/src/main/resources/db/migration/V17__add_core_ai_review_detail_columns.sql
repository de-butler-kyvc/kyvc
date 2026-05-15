ALTER TABLE kyc_applications
    ADD COLUMN IF NOT EXISTS core_ai_assessment_json TEXT,
    ADD COLUMN IF NOT EXISTS core_ai_review_raw_json TEXT;
