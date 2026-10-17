package com.cohort.api.service.mail;

import com.cohort.api.exception.MailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.sender", havingValue = "smtp")
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;
    private final String fromAddress;
    private final String fromName;

    public SmtpMailSender(JavaMailSender javaMailSender,
                          @Value("${spring.mail.username}") String fromAddress,
                          @Value("${app.mail.from-name}") String fromName) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("APP_MAIL_SENDER=smtp needs MAIL_USERNAME and MAIL_PASSWORD");
        }
        this.javaMailSender = javaMailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            javaMailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new MailDeliveryException(e);
        }
    }
}
