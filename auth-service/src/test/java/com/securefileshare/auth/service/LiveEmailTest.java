package com.securefileshare.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Diagnostic test for verifying live Gmail SMTP connectivity without requiring databases or Redis.
 * WHAT: Connects using sender ayulodhi07@gmail.com (owner of the App Password) and sends to ayushlodhi88@gmail.com.
 * WHY: Resolves account mismatch during SMTP authentication.
 */
class LiveEmailTest {

    @Test
    void testSendLiveEmail() {
        String senderEmail = "ayushlodhi88@gmail.com";
        String recipientEmail = "ayulodhi07@gmail.com";
        String password = System.getenv("MAIL_PASSWORD");

        assertThat(password)
                .as("MAIL_PASSWORD environment variable must be set in PowerShell using $env:MAIL_PASSWORD='your-16-char-password'")
                .isNotBlank();

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(senderEmail);
        mailSender.setPassword(password);

        var props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject("SecureFileShare - Live SMTP Test");
        message.setText("If you received this email, your Gmail SMTP integration and 16-character app password are working perfectly!");

        mailSender.send(message);
        System.out.println("Live test email sent successfully from " + senderEmail + " to " + recipientEmail);
    }

    @Test
    void testSendFriendRequestEmailLive() {
        String senderEmail = "ayushlodhi88@gmail.com";
        String recipientEmail = "ayulodhi07@gmail.com";
        String password = System.getenv("MAIL_PASSWORD");

        assertThat(password).isNotBlank();

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(senderEmail);
        mailSender.setPassword(password);

        var props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject("Secure File Share - New Friend Request");
        message.setText("Hello,\n\nYou have received a new friend request from TestUserAdmin.\n\n" +
                "Log in to your Secure File Share dashboard to accept or decline.");

        mailSender.send(message);
        System.out.println("Friend request live test email sent successfully to " + recipientEmail);
    }
}
