package com.kyvc.backend.global.logging;

import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// JSON 로그 메시지 생성 유틸
public final class LogMessageBuilder {

    private static final String SERVICE_NAME = "backend";
    private static final String REQUEST_ID_KEY = "requestId";

    private final Map<String, Object> fields = new LinkedHashMap<>(); // JSON 필드 순서 보존

    private LogMessageBuilder() {
        fields.put("timestamp", OffsetDateTime.now().toString());
        fields.put("level", null);
        fields.put("service", SERVICE_NAME);
        fields.put("env", resolveEnv());
    }

    public static LogMessageBuilder create() {
        return new LogMessageBuilder();
    }

    public LogMessageBuilder requestIdFromMdc() {
        return requestId(MDC.get(REQUEST_ID_KEY));
    }

    public LogMessageBuilder level(String level // 로그 레벨
    ) {
        return field("level", level);
    }

    public LogMessageBuilder requestId(String requestId // 요청 추적 ID
    ) {
        return field("requestId", requestId);
    }

    public LogMessageBuilder env(String env // 실행 환경명
    ) {
        return field("env", env);
    }

    public LogMessageBuilder event(String event // 로그 이벤트명
    ) {
        return field("event", event);
    }

    public LogMessageBuilder message(String message // 로그 메시지
    ) {
        return field("message", message);
    }

    public LogMessageBuilder path(String path // 요청 경로
    ) {
        return field("path", path);
    }

    public LogMessageBuilder method(String method // HTTP 메서드
    ) {
        return field("method", method);
    }

    public LogMessageBuilder status(Integer status // HTTP 상태
    ) {
        return field("status", status);
    }

    public LogMessageBuilder code(String code // 응답 코드
    ) {
        return field("code", code);
    }

    public LogMessageBuilder durationMs(Long durationMs // 처리 시간 밀리초
    ) {
        return field("durationMs", durationMs);
    }

    public LogMessageBuilder exception(String exception // 예외 클래스명
    ) {
        return field("exception", exception);
    }

    public LogMessageBuilder field(
            String key, // JSON 필드명
            Object value // JSON 필드값
    ) {
        if (key == null || key.isBlank()) {
            return this;
        }
        fields.put(key, value);
        return this;
    }

    public LogMessageBuilder fields(Map<String, Object> additionalFields // 추가 로그 필드
    ) {
        if (additionalFields == null) {
            return this;
        }

        for (Map.Entry<String, Object> entry : additionalFields.entrySet()) {
            field(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public String build() {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            if (!first) {
                json.append(',');
            }
            first = false;

            json.append('"')
                    .append(escape(entry.getKey()))
                    .append("\":");
            appendValue(json, entry.getValue());
        }

        return json.append('}').toString();
    }

    // JSON 타입별 값 출력
    private void appendValue(
            StringBuilder json, // JSON 문자열 버퍼
            Object value // JSON 필드값
    ) {
        if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
            return;
        }

        json.append('"')
                .append(escape(String.valueOf(value)))
                .append('"');
    }

    // JSON 문자열 특수문자 이스케이프
    private String escape(String value // 원본 문자열
    ) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i); // 검사 대상 문자
            switch (current) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> appendEscapedCharacter(escaped, current);
            }
        }
        return escaped.toString();
    }

    // 제어 문자 유니코드 이스케이프
    private void appendEscapedCharacter(
            StringBuilder escaped, // 이스케이프 결과 버퍼
            char current // 검사 대상 문자
    ) {
        if (current < 0x20) {
            escaped.append(String.format("\\u%04x", (int) current));
            return;
        }
        escaped.append(current);
    }

    // 실행 환경명 결정
    private static String resolveEnv() {
        String springProfile = normalize(System.getProperty("spring.profiles.active")); // JVM 프로필 옵션
        if (springProfile != null) {
            return springProfile;
        }

        String envProfile = normalize(System.getenv("SPRING_PROFILES_ACTIVE")); // Spring 프로필 환경변수
        if (envProfile != null) {
            return envProfile;
        }

        String env = normalize(System.getenv("APP_ENV")); // 애플리케이션 환경명
        if (env != null) {
            return env;
        }

        return "default";
    }

    // 공백 문자열 제거
    private static String normalize(String value // 원본 문자열
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
