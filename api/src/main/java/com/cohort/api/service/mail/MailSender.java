package com.cohort.api.service.mail;

/** Strategy for outbound transactional mail; deployments swap the implementation without touching callers. */
public interface MailSender {

    void send(String to, String subject, String body);
}
