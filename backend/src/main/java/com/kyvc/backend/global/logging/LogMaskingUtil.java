package com.kyvc.backend.global.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 로그 민감정보 마스킹 유틸
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LogMaskingUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MASKED_VALUE = "***";
    private static final String TRUNCATED_SUFFIX = "...[truncated]";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([A-Za-z0-9._%+-])([A-Za-z0-9._%+-]*)(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})"
    );
    private static final Pattern JSON_SENSITIVE_VALUE_PATTERN = Pattern.compile(
            "(?i)(\"(?:[^\"]*(?:password|token|authorization|cookie|jwt|secret|privateKey|apiKey|api[-_]?key|x[-_]?api[-_]?key|verificationCode|contentBase64|documentContentBase64|attachmentContentBase64)[^\"]*|presentation|credentialJwt|credential|vc|vp|file|document|documentContent|originalContent)\"\\s*:\\s*\")([^\"]*)(\")"
    );
    private static final Pattern FORM_SENSITIVE_VALUE_PATTERN = Pattern.compile(
            "(?i)((?:[A-Za-z0-9_-]*(?:password|token|authorization|cookie|jwt|secret|privateKey|apiKey|api[-_]?key|x[-_]?api[-_]?key|verificationCode|contentBase64|documentContentBase64|attachmentContentBase64)[A-Za-z0-9_-]*|presentation|credentialJwt|credential|vc|vp|file|document|documentContent|originalContent)=)([^&\\s]*)"
    );
    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
            "password",
            "currentpassword",
            "newpassword",
            "newpasswordconfirm",
            "authorization",
            "cookie",
            "accesstoken",
            "refreshtoken",
            "token",
            "mfatoken",
            "verificationcode",
            "vpjwt",
            "presentation",
            "credentialjwt",
            "credential",
            "vc",
            "vp",
            "privatekey",
            "secret",
            "apikey",
            "xapikey",
            "file",
            "document",
            "documentcontent",
            "originalcontent",
            "contentbase64",
            "documentcontentbase64",
            "attachmentcontentbase64"
    );

    public static String maskBody(
            String body, // 로그 대상 본문
            int maxLength // 최대 출력 길이
    ) {
        if (!StringUtils.hasText(body)) {
            return null;
        }

        String maskedBody = maskStructuredBody(body); // 마스킹된 본문
        return truncate(maskedBody, maxLength);
    }

    public static String maskText(
            String text, // 로그 대상 문자열
            int maxLength // 최대 출력 길이
    ) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return truncate(maskPlainText(text), maxLength);
    }

    public static Object maskValue(Object value // 로그 필드값
    ) {
        return maskValueByKey(null, value);
    }

    public static String extractCode(String body // 응답 본문
    ) {
        if (!StringUtils.hasText(body)) {
            return null;
        }

        try {
            Object parsedBody = OBJECT_MAPPER.readValue(body, Object.class); // 파싱된 응답 본문
            Object code = findCodeValue(parsedBody); // 응답 코드 값
            return code instanceof String value && StringUtils.hasText(value) ? value : null;
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    public static boolean isSensitiveField(String fieldName // 로그 필드명
    ) {
        String normalizedFieldName = normalizeFieldName(fieldName); // 비교용 필드명
        if (normalizedFieldName == null) {
            return true;
        }

        return SENSITIVE_FIELD_NAMES.contains(normalizedFieldName)
                || normalizedFieldName.contains("password")
                || normalizedFieldName.contains("authorization")
                || normalizedFieldName.contains("cookie")
                || normalizedFieldName.contains("jwt")
                || normalizedFieldName.contains("secret")
                || normalizedFieldName.contains("privatekey")
                || normalizedFieldName.endsWith("token")
                || normalizedFieldName.endsWith("apikey");
    }

    private static String maskStructuredBody(String body // 로그 대상 본문
    ) {
        try {
            Object parsedBody = OBJECT_MAPPER.readValue(body, Object.class); // 파싱된 본문
            Object maskedBody = maskValue(parsedBody); // 마스킹된 본문 객체
            return OBJECT_MAPPER.writeValueAsString(maskedBody);
        } catch (JsonProcessingException exception) {
            return maskPlainText(body);
        }
    }

    private static Object maskValueByKey(
            String key, // 로그 필드명
            Object value // 로그 필드값
    ) {
        if (value == null) {
            return null;
        }
        if (key != null && isSensitiveField(key)) {
            return MASKED_VALUE;
        }
        if (value instanceof Map<?, ?> mapValue) {
            return maskMap(mapValue);
        }
        if (value instanceof List<?> listValue) {
            return maskList(listValue);
        }
        if (value instanceof String stringValue) {
            return maskPlainText(stringValue);
        }
        return value;
    }

    private static Map<String, Object> maskMap(Map<?, ?> mapValue // 로그 Map 값
    ) {
        Map<String, Object> maskedMap = new LinkedHashMap<>(); // 마스킹된 Map
        for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
            String key = String.valueOf(entry.getKey()); // 로그 필드명
            maskedMap.put(key, maskValueByKey(key, entry.getValue()));
        }
        return maskedMap;
    }

    private static List<Object> maskList(List<?> listValue // 로그 List 값
    ) {
        List<Object> maskedList = new ArrayList<>(); // 마스킹된 List
        for (Object item : listValue) {
            maskedList.add(maskValueByKey(null, item));
        }
        return maskedList;
    }

    private static String maskPlainText(String text // 로그 대상 문자열
    ) {
        String maskedText = maskPattern(text, JSON_SENSITIVE_VALUE_PATTERN, "$1" + MASKED_VALUE + "$3");
        maskedText = maskPattern(maskedText, FORM_SENSITIVE_VALUE_PATTERN, "$1" + MASKED_VALUE);
        return maskEmail(maskedText);
    }

    private static String maskPattern(
            String text, // 로그 대상 문자열
            Pattern pattern, // 마스킹 패턴
            String replacement // 치환 문자열
    ) {
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll(replacement);
    }

    private static String maskEmail(String text // 로그 대상 문자열
    ) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        StringBuilder maskedText = new StringBuilder(); // 이메일 마스킹 결과
        while (matcher.find()) {
            matcher.appendReplacement(
                    maskedText,
                    Matcher.quoteReplacement(matcher.group(1) + "***" + matcher.group(3))
            );
        }
        matcher.appendTail(maskedText);
        return maskedText.toString();
    }

    private static String truncate(
            String text, // 로그 대상 문자열
            int maxLength // 최대 출력 길이
    ) {
        if (maxLength <= 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + TRUNCATED_SUFFIX;
    }

    private static Object findCodeValue(Object value // 응답 본문 값
    ) {
        if (value instanceof Map<?, ?> mapValue) {
            Object codeValue = mapValue.get("code"); // 표준 응답 코드
            if (codeValue != null) {
                return codeValue;
            }
            for (Object childValue : mapValue.values()) {
                Object foundValue = findCodeValue(childValue); // 하위 응답 코드
                if (foundValue != null) {
                    return foundValue;
                }
            }
        }
        if (value instanceof List<?> listValue) {
            for (Object item : listValue) {
                Object foundValue = findCodeValue(item); // 목록 하위 응답 코드
                if (foundValue != null) {
                    return foundValue;
                }
            }
        }
        return null;
    }

    private static String normalizeFieldName(String fieldName // 로그 필드명
    ) {
        if (fieldName == null || fieldName.isBlank()) {
            return null;
        }
        return fieldName.replace("_", "")
                .replace("-", "")
                .replace(".", "")
                .toLowerCase(Locale.ROOT);
    }
}
