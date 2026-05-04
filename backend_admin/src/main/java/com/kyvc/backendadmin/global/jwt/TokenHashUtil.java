package com.kyvc.backendadmin.global.jwt;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

// 토큰 해시 유틸
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TokenHashUtil {

    // SHA-256 해시 생성
    public static String sha256(String rawToken // 원본 토큰
    ) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256"); // SHA-256 다이제스트
            byte[] digest = messageDigest.digest(Objects.requireNonNull(rawToken, "rawToken must not be null")
                    .getBytes(StandardCharsets.UTF_8)); // 토큰 해시 바이트
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }
}
