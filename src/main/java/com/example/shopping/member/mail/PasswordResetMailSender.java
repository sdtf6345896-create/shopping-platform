package com.example.shopping.member.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetMailSender {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailSender.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean mailConfigured;

    public PasswordResetMailSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                                    @Value("${spring.mail.host:}") String mailHost,
                                    @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.fromAddress = fromAddress;
        this.mailConfigured = mailSender != null && !mailHost.isBlank();
    }

    public void sendResetLink(String toEmail, String resetLink) {
        if (mailConfigured) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(toEmail);
                message.setSubject("重設您的購物平台密碼");
                message.setText("您好,\n\n請點擊以下連結重設密碼(30 分鐘內有效):\n" + resetLink
                        + "\n\n若非您本人操作,請忽略此信件。");
                mailSender.send(message);
                return;
            } catch (MailException ex) {
                log.warn("寄送密碼重設信失敗,改以 log 模擬輸出。原因:{}", ex.getMessage());
            }
        }

        // 未設定 SMTP(或寄送失敗)時,以 log 模擬寄信,比照付款流程的模擬方式
        log.info("[模擬寄信] 密碼重設連結給 {}:{}", toEmail, resetLink);
    }
}
