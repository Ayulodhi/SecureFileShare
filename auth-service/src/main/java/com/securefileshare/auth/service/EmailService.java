package com.securefileshare.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for sending transactional emails via Gmail SMTP asynchronously.
 * WHAT: Constructs and dispatches verification emails, friend request notices, and document share alerts without blocking caller transactions.
 * WHY: Prevents SMTP latency or failures from blocking HTTP worker threads or rolling back database transactions, while returning CompletableFuture for explicit error handling and failure tracking.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String appBaseUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:noreply@securefileshare.com}") String fromEmail,
            @Value("${app.base-url:http://localhost:3000}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.appBaseUrl = appBaseUrl;
    }

    public EmailService(JavaMailSender mailSender, String fromEmail) {
        this(mailSender, fromEmail, "http://localhost:3000");
    }

    /**
     * Sends account email verification link / token asynchronously.
     */
    @Async
    public CompletableFuture<Void> sendVerificationEmail(String toEmail, String verificationToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Secure File Share - Email Verification");
            message.setText("Please verify your account using the following token:\n\n" + verificationToken);
            mailSender.send(message);
            log.info("Verification email sent successfully to {}", toEmail);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Sends friend request notification to recipient email asynchronously.
     */
    @Async
    public CompletableFuture<Void> sendFriendRequestEmail(String toEmail, String senderUsername) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Secure File Share - New Friend Request");
            message.setText("Hello,\n\nYou have received a new friend request from " + senderUsername +
                    ". Log in to your Secure File Share dashboard to accept or decline.");
            mailSender.send(message);
            log.info("Friend request email sent successfully to {}", toEmail);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send friend request email to {}: {}", toEmail, e.getMessage());
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Sends document sharing notification digest asynchronously.
     */
    @Async
    public CompletableFuture<Void> sendDocumentShareEmail(String toEmail, String senderUsername, String documentName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Secure File Share - Document Shared With You");
            message.setText("Hello,\n\n" + senderUsername + " has shared a secure document (" + documentName +
                    ") with you. Log in to your dashboard to view and decrypt it.");
            mailSender.send(message);
            log.info("Document share email sent successfully to {}", toEmail);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send document share email to {}: {}", toEmail, e.getMessage());
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Sends document sharing invite email to non-registered recipient asynchronously.
     */
    @Async
    public CompletableFuture<Void> sendDocumentShareInviteEmail(String toEmail, String senderUsername, String documentName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Secure File Share - Action Required: Document Shared With You");
            message.setText("Hello,\n\n" + senderUsername + " has shared a secure document (" + documentName +
                    ") with you on Secure File Share.\n\n" +
                    "You do not have an account yet. Please sign up at " + appBaseUrl + "/register to create an account and decrypt your shared file.");
            mailSender.send(message);
            log.info("Document share invite email sent successfully to {}", toEmail);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send document share invite email to {}: {}", toEmail, e.getMessage());
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Sends friend invitation email to non-registered recipient asynchronously.
     */
    @Async
    public CompletableFuture<Void> sendFriendInviteEmail(String toEmail, String senderUsername) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Secure File Share - Friend Invitation");
            message.setText("Hello,\n\n" + senderUsername + " has invited you to connect on Secure File Share.\n\n" +
                    "Please sign up at " + appBaseUrl + "/register to create your account and connect with them.");
            mailSender.send(message);
            log.info("Friend invite email sent successfully to unregistered recipient {}", toEmail);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send friend invite email to {}: {}", toEmail, e.getMessage());
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
