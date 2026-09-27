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
class EmailVerificationMailSenderTest {

    @Mock
    private MailSender mailSender;

    @Test
    void sendVerificationLink_delegatesToMailSender_withLinkInBody() {
        EmailVerificationMailSender sender = new EmailVerificationMailSender(mailSender);

        sender.sendVerificationLink("member@example.com", "http://localhost:5173/verify-email?token=abc");

        verify(mailSender).send(
                eq("member@example.com"),
                argThat(subject -> subject.contains("驗證")),
                argThat(body -> body.contains("http://localhost:5173/verify-email?token=abc")));
    }
}
