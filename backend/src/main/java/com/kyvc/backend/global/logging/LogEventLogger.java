package com.kyvc.backend.global.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// 업무 코드용 JSON 로그 컴포넌트
@Slf4j
@Component
@RequiredArgsConstructor
public class LogEventLogger {

    private static final Set<String> RESERVED_FIELDS = Set.of(
            "timestamp",
            "service",
            "env",
            "level",
            "requestid",
            "event",
            "message"
    );

    private final Environment environment;

    public void info(
            String event, // 로그 이벤트명
            String message // 로그 메시지
    ) {
        info(event, message, null);
    }

    public void info(
            String event, // 로그 이벤트명
            String message, // 로그 메시지
            Map<String, Object> fields // 추가 로그 필드
    ) {
        log.info(buildLogMessage("INFO", event, message, fields, null));
    }

    public void warn(
            String event, // 로그 이벤트명
            String message // 로그 메시지
    ) {
        warn(event, message, null);
    }

    public void warn(
            String event, // 로그 이벤트명
            String message, // 로그 메시지
            Map<String, Object> fields // 추가 로그 필드
    ) {
        log.warn(buildLogMessage("WARN", event, message, fields, null));
    }

    public void error(
            String event, // 로그 이벤트명
            String message // 로그 메시지
    ) {
        error(event, message, null, null);
    }

    public void error(
            String event, // 로그 이벤트명
            String message, // 로그 메시지
            Throwable throwable // 원인 예외
    ) {
        error(event, message, null, throwable);
    }

    public void error(
            String event, // 로그 이벤트명
            String message, // 로그 메시지
            Map<String, Object> fields // 추가 로그 필드
    ) {
        error(event, message, fields, null);
    }

    public void error(
            String event, // 로그 이벤트명
            String message, // 로그 메시지
            Map<String, Object> fields, // 추가 로그 필드
            Throwable throwable // 원인 예외
    ) {
        String logMessage = buildLogMessage("ERROR", event, message, fields, throwable); // 오류 로그 메시지
        if (throwable == null) {
            log.error(logMessage);
            return;
        }
        log.error(logMessage, throwable);
    }

    // 공통 로그 메시지 생성
    private String buildLogMessage(
            String level, // 로그 레벨
            String event, // 로그 이벤트명
            String message, // 로그 메시지
            Map<String, Object> fields, // 추가 로그 필드
            Throwable throwable // 예외 클래스 기준
    ) {
        LogMessageBuilder builder = LogMessageBuilder.create()
                .level(level)
                .env(resolveEnv())
                .requestIdFromMdc()
                .event(event)
                .message(message)
                .fields(sanitizeFields(fields));

        if (throwable != null) {
            builder.exception(throwable.getClass().getName());
        }

        return builder.build();
    }

    // 민감정보 필드 제거
    private Map<String, Object> sanitizeFields(Map<String, Object> fields // 추가 로그 필드
    ) {
        if (fields == null) {
            return null;
        }

        Map<String, Object> safeFields = new LinkedHashMap<>(); // 출력 허용 필드
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldName = entry.getKey(); // 원본 필드명
            Object fieldValue = entry.getValue(); // 원본 필드값
            if (fieldValue == null || isReservedField(fieldName) || isSensitiveField(fieldName)) {
                continue;
            }
            safeFields.put(fieldName, LogMaskingUtil.maskValue(fieldValue));
        }
        return safeFields;
    }

    // 공통 필드 덮어쓰기 방지
    private boolean isReservedField(String fieldName // 로그 필드명
    ) {
        String normalized = normalizeFieldName(fieldName);
        return normalized == null || RESERVED_FIELDS.contains(normalized);
    }

    // 민감정보 필드명 판별
    private boolean isSensitiveField(String fieldName // 로그 필드명
    ) {
        return LogMaskingUtil.isSensitiveField(fieldName);
    }

    // 필드명 비교용 정규화
    private String normalizeFieldName(String fieldName // 로그 필드명
    ) {
        if (fieldName == null || fieldName.isBlank()) {
            return null;
        }
        return fieldName.replace("_", "")
                .replace("-", "")
                .replace(".", "")
                .toLowerCase(Locale.ROOT);
    }

    // 현재 실행 환경명 조회
    private String resolveEnv() {
        String[] activeProfiles = environment.getActiveProfiles(); // 활성 Spring 프로필
        if (activeProfiles.length > 0) {
            return String.join(",", activeProfiles);
        }

        String propertyProfile = normalize(environment.getProperty("spring.profiles.active")); // 환경 프로필 속성
        if (propertyProfile != null) {
            return propertyProfile;
        }

        String envProfile = normalize(System.getenv("SPRING_PROFILES_ACTIVE")); // Spring 프로필 환경변수
        if (envProfile != null) {
            return envProfile;
        }

        String appEnv = normalize(System.getenv("APP_ENV")); // 애플리케이션 환경명
        if (appEnv != null) {
            return appEnv;
        }

        return "default";
    }

    // 공백 문자열 제거
    private String normalize(String value // 원본 문자열
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
