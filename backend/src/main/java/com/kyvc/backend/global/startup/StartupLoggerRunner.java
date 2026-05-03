package com.kyvc.backend.global.startup;

import com.kyvc.backend.global.logging.LogMessageBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

// 애플리케이션 시작 로그 실행기
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupLoggerRunner implements ApplicationRunner {

    private static final Logger startupConsoleLog = LoggerFactory.getLogger("KYVC_STARTUP_CONSOLE");
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    private final Environment environment;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    @Override
    public void run(ApplicationArguments args // 실행 인자
    ) {
        DbHealth dbHealth = checkDbHealth(); // DB 연결 상태
        log.info(LogMessageBuilder.create()
                .level("INFO")
                .env(resolveEnv())
                .event("application.started")
                .message("Application started")
                .field("app", environment.getProperty("spring.application.name", "backend"))
                .field("activeProfiles", Arrays.toString(resolveActiveProfiles()))
                .field("serverPort", environment.getProperty("server.port", "8080"))
                .field("dbStatus", dbHealth.status())
                .build());

        logConsoleStartupDetails(dbHealth);

        if (!dbHealth.up()) {
            log.error(LogMessageBuilder.create()
                    .level("ERROR")
                    .env(resolveEnv())
                    .event("application.db.down")
                    .message("Database connection check failed")
                    .field("dbStatus", dbHealth.status())
                    .field("failureReason", dbHealth.failureReason())
                    .build());
        }
    }

    // 콘솔 전용 시작 상세 로그 출력
    private void logConsoleStartupDetails(DbHealth dbHealth // DB 연결 상태
    ) {
        startupConsoleLog.info("==================================================");
        startupConsoleLog.info("[Application Started]");
        startupConsoleLog.info("애플리케이션 이름: {}", environment.getProperty("spring.application.name", "backend"));
        startupConsoleLog.info("실행 프로필: {}", Arrays.toString(resolveActiveProfiles()));
        startupConsoleLog.info("서버 포트: {}", environment.getProperty("server.port", "8080"));
        startupConsoleLog.info("Context Path: {}", environment.getProperty("server.servlet.context-path", "/"));
        startupConsoleLog.info("로그 경로: {}", environment.getProperty("LOG_PATH", "/app/logs"));
        startupConsoleLog.info("Dev 토큰 활성화 여부: {}", environment.getProperty("kyvc.dev-token.enabled", "false"));
        startupConsoleLog.info("파일 저장 루트 경로: {}", environment.getProperty("app.storage.root", "(설정되지 않음)"));
        startupConsoleLog.info("프로세스 ID: {}", ProcessHandle.current().pid());
        startupConsoleLog.info("JVM Uptime: {}", formatDuration(ManagementFactory.getRuntimeMXBean().getUptime()));
        startupConsoleLog.info("Java 버전: {}", System.getProperty("java.version"));
        startupConsoleLog.info("운영체제: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        startupConsoleLog.info("OS 아키텍처: {}", System.getProperty("os.arch"));
        startupConsoleLog.info("TimeZone: {}", TimeZone.getDefault().getID());
        startupConsoleLog.info("Locale: {}", Locale.getDefault().toLanguageTag());
        startupConsoleLog.info("Max Heap Memory: {} MB", Runtime.getRuntime().maxMemory() / BYTES_PER_MEGABYTE);
        startupConsoleLog.info("CPU Core Count: {}", Runtime.getRuntime().availableProcessors());
        startupConsoleLog.info("DB 연결 상태: {}", dbHealth.status());
        startupConsoleLog.info("==================================================");
    }

    // 활성 프로필 문자열 배열
    private String[] resolveActiveProfiles() {
        String[] activeProfiles = environment.getActiveProfiles(); // 활성 Spring 프로필
        if (activeProfiles.length == 0) {
            return new String[]{"default"};
        }
        return activeProfiles;
    }

    // 실행 환경명 조회
    private String resolveEnv() {
        String[] activeProfiles = resolveActiveProfiles(); // 활성 Spring 프로필
        if (activeProfiles.length == 0) {
            return "default";
        }
        return String.join(",", activeProfiles);
    }

    // DB 연결 상태 확인
    private DbHealth checkDbHealth() {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable(); // 선택적 JdbcTemplate
        if (jdbcTemplate == null) {
            return new DbHealth("실패", false, "JdbcTemplate 없음");
        }

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return new DbHealth("정상", true, null);
        } catch (Exception exception) {
            return new DbHealth("실패", false, exception.getClass().getSimpleName());
        }
    }

    // 실행 시간 문자열 변환
    private String formatDuration(long uptimeMs // JVM 실행 시간 밀리초
    ) {
        Duration duration = Duration.ofMillis(uptimeMs); // JVM 시작 후 경과 시간
        return "%d초".formatted(duration.toSeconds());
    }

    private record DbHealth(
            String status, // DB 상태
            boolean up, // DB 연결 성공 여부
            String failureReason // DB 연결 실패 사유
    ) {
    }
}
