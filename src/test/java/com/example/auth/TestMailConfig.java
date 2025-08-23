package com.example.auth;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;

@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return new NoOpMailSender();
    }

    static class NoOpMailSender implements JavaMailSender {
        @Override
        public MimeMessage createMimeMessage() { return null; }
        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) { return null; }
        @Override
        public void send(MimeMessage mimeMessage) throws MailException { /* no-op */ }
        @Override
        public void send(MimeMessage[] mimeMessages) throws MailException { /* no-op */ }
        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException { /* no-op */ }
        @Override
        public void send(SimpleMailMessage[] simpleMessages) throws MailException { /* no-op */ }
    }
}


