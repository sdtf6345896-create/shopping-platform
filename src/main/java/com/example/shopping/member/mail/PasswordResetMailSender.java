package com.example.shopping.member.mail;

import com.example.shopping.notification.mail.MailSender;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetMailSender {

    private final MailSender mailSender;

    public PasswordResetMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** 密碼被修改後的安全通知,讓本人能及早發現帳號被盜用 */
    public void sendPasswordChangedNotice(String toEmail) {
        String body = "您好,\n\n您的購物平台帳號密碼剛剛已變更,所有裝置都需要重新登入。"
                + "\n\n若不是您本人操作,請立即使用「忘記密碼」重設密碼並聯繫客服。";
        mailSender.send(toEmail, "您的帳號密碼已變更", body);
    }

    public void sendResetLink(String toEmail, String resetLink) {
        String body = "您好,\n\n請點擊以下連結重設密碼(30 分鐘內有效):\n" + resetLink
                + "\n\n若非您本人操作,請忽略此信件。";
        mailSender.send(toEmail, "重設您的購物平台密碼", body);
    }
}
