package com.example.shopping.notification.mail;

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
class MailSenderTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void send_sendsRealEmail_whenSmtpConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        MailSender sender = new MailSender(mailSenderProvider, "smtp.example.com", "noreply@example.com");

        sender.send("member@example.com", "主旨", "內容");

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_fallsBackToLog_whenHostNotConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        MailSender sender = new MailSender(mailSenderProvider, "", "noreply@example.com");

        sender.send("member@example.com", "主旨", "內容");

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_fallsBackToLog_whenNoMailSenderBean() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        MailSender sender = new MailSender(mailSenderProvider, "smtp.example.com", "noreply@example.com");

        // 沒有 JavaMailSender bean 時不該丟例外,直接退回 log 模擬
        sender.send("member@example.com", "主旨", "內容");
    }

    @Test
    void send_fallsBackToLog_whenSendThrows() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        doThrow(new MailSendException("connection refused")).when(javaMailSender).send(any(SimpleMailMessage.class));
        MailSender sender = new MailSender(mailSenderProvider, "smtp.example.com", "noreply@example.com");

        // 寄送失敗不應該讓例外往上拋
        sender.send("member@example.com", "主旨", "內容");
    }
}
