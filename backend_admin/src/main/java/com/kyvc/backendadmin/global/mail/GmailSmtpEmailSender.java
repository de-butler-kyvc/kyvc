package com.kyvc.backendadmin.global.mail;

import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.logging.LogEventLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Backend Admin 메일 발송을 처리하는 SMTP 이메일 발송 구현체입니다.
 */
@Component
@RequiredArgsConstructor
public class GmailSmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final LogEventLogger logEventLogger;

    @Override
    public void send(
            String to,
            String subject,
            String body
    ) {
        if (!StringUtils.hasText(to) || !StringUtils.hasText(subject) || !StringUtils.hasText(body)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (!mailProperties.hasFrom()) {
            throw new ApiException(ErrorCode.EMAIL_CONFIGURATION_INVALID);
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom().trim());
        message.setTo(to.trim());
        message.setSubject(subject.trim());
        message.setText(body);

        try {
            javaMailSender.send(message);
            logEventLogger.info(
                    "ADMIN_EMAIL_SEND_SUCCESS",
                    "admin email sent",
                    Map.of("target", maskEmail(to))
            );
        } catch (MailException exception) {
            logEventLogger.error(
                    "ADMIN_EMAIL_SEND_FAILED",
                    "admin email send failed",
                    Map.of("target", maskEmail(to)),
                    exception
            );
            throw new ApiException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "***";
        }

        String normalized = email.trim();
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        String localPart = normalized.substring(0, atIndex);
        String domainPart = normalized.substring(atIndex);
        if (localPart.length() == 1) {
            return localPart + "***" + domainPart;
        }
        return localPart.substring(0, 1) + "***" + domainPart;
    }
}
