package com.example.shopping.question.mail;

import com.example.shopping.notification.mail.MailSender;
import com.example.shopping.question.entity.ProductQuestion;
import org.springframework.stereotype.Component;

@Component
public class QuestionMailSender {

    private final MailSender mailSender;

    public QuestionMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void notifyAnswered(ProductQuestion question) {
        String body = String.format("您好 %s,%n%n您在「%s」提出的問題已獲得回覆:%n%n問:%s%n答:%s",
                question.getMember().getName(), question.getProduct().getName(),
                question.getContent(), question.getAnswer());
        mailSender.send(question.getMember().getEmail(), "您的商品提問已回覆", body);
    }
}
