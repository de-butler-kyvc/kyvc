package com.kyvc.backend.global.mail;

// 이메일 발송 인터페이스
public interface EmailSender {

    void send(
            String to, // 수신자 이메일
            String subject, // 이메일 제목
            String body // 이메일 본문
    );
}
