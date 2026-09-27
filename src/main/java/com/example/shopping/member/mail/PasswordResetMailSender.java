package com.example.shopping.member.mail;

import com.example.shopping.notification.mail.MailSender;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetMailSender {

    private final MailSender mailSender;

    public PasswordResetMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendResetLink(String toEmail, String resetLink) {
        String body = "您好,\n\n請點擊以下連結重設密碼(30 分鐘內有效):\n" + resetLink
                + "\n\n若非您本人操作,請忽略此信件。";
        mailSender.send(toEmail, "重設您的購物平台密碼", body);
    }
}
