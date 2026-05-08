package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.CorporateDocument;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 법인 문서 JPA Repository
 */
public interface CorporateDocumentJpaRepository extends JpaRepository<CorporateDocument, Long> {
}
