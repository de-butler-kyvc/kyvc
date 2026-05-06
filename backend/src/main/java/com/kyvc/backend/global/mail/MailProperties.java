package com.kyvc.backend.global.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// 이메일 설정 속성
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
