package com.kyvc.backend.global.mail;

import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// Gmail SMTP 기반 이메일 발송 구현체
@Slf4j
@Component
@RequiredArgsConstructor
public class GmailSmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public void send(
            String to, // 수신자 이메일
            String subject, // 이메일 제목
            String body // 이메일 본문
    ) {
        if (!StringUtils.hasText(to) || !StringUtils.hasText(subject) || !StringUtils.hasText(body)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (!mailProperties.hasFrom()) {
            throw new ApiException(ErrorCode.EMAIL_CONFIGURATION_INVALID);
        }

        SimpleMailMessage message = new SimpleMailMessage(); // 발송 메일 메시지
        message.setFrom(mailProperties.getFrom().trim());
        message.setTo(to.trim());
        message.setSubject(subject.trim());
        message.setText(body);

        try {
            javaMailSender.send(message);
            log.info("email send success. target={}", maskEmail(to));
        } catch (MailException exception) {
            log.error("email send failed. target={}", maskEmail(to), exception);
            throw new ApiException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String maskEmail(
            String email // 마스킹 대상 이메일
    ) {
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
