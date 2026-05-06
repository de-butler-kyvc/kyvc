package com.kyvc.backendadmin.global.mail;

/**
 * Backend Admin 인증 흐름에서 사용하는 이메일 발송 인터페이스입니다.
 */
public interface EmailSender {

    /**
     * 일반 텍스트 이메일을 발송합니다.
     *
     * @param to 수신자 이메일 주소
     * @param subject 이메일 제목
     * @param body 이메일 본문
     */
    void send(String to, String subject, String body);
}
