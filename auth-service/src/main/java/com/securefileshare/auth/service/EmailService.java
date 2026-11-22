package com.securefileshare.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending transactional emails via Gmail SMTP asynchronously.
 * WHAT: Constructs and dispatches verification emails, friend request notices, and document share alerts without blocking caller transactions.
 * WHY: Prevents SMTP latency or failures from blocking HTTP worker threads or rolling back database transactions.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:noreply@securefileshare.com}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    /**
     * Sends account email verification link / token asynchronously.
     */
    @Async
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Secure File Share - Email Verification");
            message.setText("Please verify your account using the following token:\n\n" + verificationToken);
            mailSender.send(message);
            log.info("Verification email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends friend request notification to recipient email asynchronously.
     */
    @Async
    public void sendFriendRequestEmail(String toEmail, String senderUsername) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Secure File Share - New Friend Request");
            message.setText("Hello,\n\nYou have received a new friend request from " + senderUsername +
                    ". Log in to your Secure File Share dashboard to accept or decline.");
            mailSender.send(message);
            log.info("Friend request email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send friend request email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends document sharing notification digest asynchronously.
     */
    @Async
    public void sendDocumentShareEmail(String toEmail, String senderUsername, String documentName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Secure File Share - Document Shared With You");
            message.setText("Hello,\n\n" + senderUsername + " has shared a secure document (" + documentName +
                    ") with you. Log in to your dashboard to view and decrypt it.");
            mailSender.send(message);
            log.info("Document share email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send document share email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends document sharing invite email to non-registered recipient asynchronously.
     */
    @Async
    public void sendDocumentShareInviteEmail(String toEmail, String senderUsername, String documentName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Secure File Share - Action Required: Document Shared With You");
            message.setText("Hello,\n\n" + senderUsername + " has shared a secure document (" + documentName +
                    ") with you on Secure File Share.\n\n" +
                    "You do not have an account yet. Please sign up at http://localhost:3000/register to create an account and decrypt your shared file.");
            mailSender.send(message);
            log.info("Document share invite email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send document share invite email to {}: {}", toEmail, e.getMessage());
        }
    }
}
