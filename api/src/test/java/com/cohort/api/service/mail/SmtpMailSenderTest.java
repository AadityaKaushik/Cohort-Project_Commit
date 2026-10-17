package com.cohort.api.service.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cohort.api.exception.MailDeliveryException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpMailSenderTest {

    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        javaMailSender = mock(JavaMailSender.class);
        when(javaMailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void sendsAPlainTextMessageFromTheConfiguredAccount() throws Exception {
        new SmtpMailSender(javaMailSender, "cohort.sender@gmail.com", "Cohort")
                .send("someone@example.com", "Your Cohort sign-up code", "Your code is 123456");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(sent.capture());
        MimeMessage message = sent.getValue();
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("cohort.sender@gmail.com");
        assertThat(from.getPersonal()).isEqualTo("Cohort");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("someone@example.com");
        assertThat(message.getSubject()).isEqualTo("Your Cohort sign-up code");
        assertThat(message.getContent().toString()).contains("123456");
    }

    @Test
    void aFailedSendBecomesAMailDeliveryError() {
        doThrow(new MailSendException("connection refused")).when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> new SmtpMailSender(javaMailSender, "cohort.sender@gmail.com", "Cohort")
                .send("someone@example.com", "Subject", "Body"))
                .isInstanceOf(MailDeliveryException.class);
    }

    @Test
    void refusesToStartWithoutASenderAccount() {
        assertThatThrownBy(() -> new SmtpMailSender(javaMailSender, "", "Cohort"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_USERNAME");
    }
}
