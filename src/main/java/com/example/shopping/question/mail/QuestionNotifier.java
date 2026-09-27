package com.example.shopping.question.mail;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.notification.mail.MailSender;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.question.entity.ProductQuestion;
import org.springframework.stereotype.Component;

/** 提問回覆通知:同時寄 email 並建立站內通知 */
@Component
public class QuestionNotifier {

    private final MailSender mailSender;
    private final NotificationService notificationService;

    public QuestionNotifier(MailSender mailSender, NotificationService notificationService) {
        this.mailSender = mailSender;
        this.notificationService = notificationService;
    }

    public void notifyAnswered(ProductQuestion question) {
        String body = String.format("您好 %s,%n%n您在「%s」提出的問題已獲得回覆:%n%n問:%s%n答:%s",
                question.getMember().getName(), question.getProduct().getName(),
                question.getContent(), question.getAnswer());
        mailSender.send(question.getMember().getEmail(), "您的商品提問已回覆", body);
        notificationService.notify(question.getMember().getId(), NotificationType.QUESTION, "您的商品提問已回覆",
                "「" + question.getProduct().getName() + "」:" + question.getAnswer(),
                "/products/" + question.getProduct().getId());
    }
}
