package com.example.shopping.member.mail;

import com.example.shopping.notification.mail.MailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetMailSenderTest {

    @Mock
    private MailSender mailSender;

    @Test
    void sendResetLink_delegatesToMailSender_withLinkInBody() {
        PasswordResetMailSender sender = new PasswordResetMailSender(mailSender);

        sender.sendResetLink("member@example.com", "http://localhost:5173/reset-password?token=abc");

        verify(mailSender).send(
                eq("member@example.com"),
                argThat(subject -> subject.contains("密碼")),
                argThat(body -> body.contains("http://localhost:5173/reset-password?token=abc")));
    }
}
