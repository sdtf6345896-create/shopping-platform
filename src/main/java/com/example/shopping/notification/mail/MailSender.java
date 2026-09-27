package com.example.shopping.notification.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 統一的寄信出口:設定 SMTP(spring.mail.host)時真的寄出,否則(或寄送失敗時)以 log 模擬,
 * 比照專案付款流程「模擬」的做法,不需要真的申請信箱也能跑完整個流程。
 */
@Component
public class MailSender {

    private static final Logger log = LoggerFactory.getLogger(MailSender.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean mailConfigured;

    public MailSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                       @Value("${spring.mail.host:}") String mailHost,
                       @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.fromAddress = fromAddress;
        this.mailConfigured = mailSender != null && !mailHost.isBlank();
    }

    public void send(String toEmail, String subject, String body) {
        if (mailConfigured) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                return;
            } catch (MailException ex) {
                log.warn("寄送 email 失敗,改以 log 模擬輸出。收件者:{},原因:{}", toEmail, ex.getMessage());
            }
        }

        log.info("[模擬寄信] To: {}, Subject: {}\n{}", toEmail, subject, body);
    }
}
