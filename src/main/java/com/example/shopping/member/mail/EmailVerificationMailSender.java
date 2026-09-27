package com.example.shopping.member.mail;

import com.example.shopping.notification.mail.MailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationMailSender {

    private final MailSender mailSender;

    public EmailVerificationMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationLink(String toEmail, String verifyLink) {
        String body = "您好,\n\n感謝註冊購物平台會員,請點擊以下連結完成 Email 驗證(24 小時內有效):\n" + verifyLink
                + "\n\n完成驗證後即可登入。若非您本人操作,請忽略此信件。";
        mailSender.send(toEmail, "請驗證您的購物平台帳號", body);
    }
}
