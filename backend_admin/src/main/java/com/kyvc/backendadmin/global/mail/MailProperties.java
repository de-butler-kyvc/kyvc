package com.kyvc.backendadmin.global.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 환경변수 기반 Spring 설정에서 로드되는 KYvC 메일 설정입니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kyvc.mail")
public class MailProperties {

    private String from = "";

    public boolean hasFrom() {
        return StringUtils.hasText(from);
    }
}
