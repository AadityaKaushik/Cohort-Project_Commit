package com.cohort.api.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.sender", havingValue = "log", matchIfMissing = true)
public class LogMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LogMailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("Mail to {} | {} | {}", to, subject, body);
    }
}
