package com.example.shopping.member.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetMailSenderTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendResetLink_sendsRealEmail_whenSmtpConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        PasswordResetMailSender sender = new PasswordResetMailSender(
                mailSenderProvider, "smtp.example.com", "noreply@example.com");

        sender.sendResetLink("member@example.com", "http://localhost:5173/reset-password?token=abc");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendResetLink_fallsBackToLog_whenHostNotConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        PasswordResetMailSender sender = new PasswordResetMailSender(
                mailSenderProvider, "", "noreply@example.com");

        sender.sendResetLink("member@example.com", "http://localhost:5173/reset-password?token=abc");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendResetLink_fallsBackToLog_whenNoMailSenderBean() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        PasswordResetMailSender sender = new PasswordResetMailSender(
                mailSenderProvider, "smtp.example.com", "noreply@example.com");

        // 沒有 JavaMailSender bean 時不該丟例外,直接退回 log 模擬
        sender.sendResetLink("member@example.com", "http://localhost:5173/reset-password?token=abc");
    }

    @Test
    void sendResetLink_fallsBackToLog_whenSendThrows() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailSendException("connection refused")).when(mailSender).send(any(SimpleMailMessage.class));
        PasswordResetMailSender sender = new PasswordResetMailSender(
                mailSenderProvider, "smtp.example.com", "noreply@example.com");

        // 寄送失敗不應該讓例外往上拋,忘記密碼流程仍要回應成功
        sender.sendResetLink("member@example.com", "http://localhost:5173/reset-password?token=abc");
    }
}
