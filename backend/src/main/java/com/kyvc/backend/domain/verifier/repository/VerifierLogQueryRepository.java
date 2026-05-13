package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.VerifierLog;
import com.kyvc.backend.global.util.KyvcEnums;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Verifier log QueryRepository
 */
public interface VerifierLogQueryRepository {

    /**
     * Verifier log 목록 조회
     *
     * @param verifierId Verifier ID
     * @param actionTypeCode 작업 유형
     * @param from 시작 일시
     * @param to 종료 일시
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return Verifier log 목록
     */
    List<VerifierLog> findLogs(
            Long verifierId, // Verifier ID
            KyvcEnums.VerifierActionType actionTypeCode, // 작업 유형
            LocalDateTime from, // 시작 일시
            LocalDateTime to, // 종료 일시
            int page, // 페이지 번호
            int size // 페이지 크기
    );

    /**
     * Verifier log 건수 조회
     *
     * @param verifierId Verifier ID
     * @param actionTypeCode 작업 유형
     * @param from 시작 일시
     * @param to 종료 일시
     * @return Verifier log 건수
     */
    long countLogs(
            Long verifierId, // Verifier ID
            KyvcEnums.VerifierActionType actionTypeCode, // 작업 유형
            LocalDateTime from, // 시작 일시
            LocalDateTime to // 종료 일시
    );

    /**
     * Verifier log 집계 대상 목록 조회
     *
     * @param verifierId Verifier ID
     * @param from 시작 일시
     * @param to 종료 일시
     * @return Verifier log 목록
     */
    List<VerifierLog> findLogsForStats(
            Long verifierId, // Verifier ID
            LocalDateTime from, // 시작 일시
            LocalDateTime to // 종료 일시
    );
}
