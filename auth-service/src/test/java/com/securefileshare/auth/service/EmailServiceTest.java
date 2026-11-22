package com.securefileshare.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for EmailService.
 * WHAT: Verifies correct email construction and dispatching via JavaMailSender.
 * WHY: Ensures verification, friend request, and document share emails contain correct recipients, subjects, and text.
 */
class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService emailService;
    private final String fromEmail = "noreply@securefileshare.com";

    @BeforeEach
    void setUp() {
        mailSender = Mockito.mock(JavaMailSender.class);
        emailService = new EmailService(mailSender, fromEmail);
    }

    @Test
    @DisplayName("Should send verification email with correct details")
    void testSendVerificationEmail() {
        String to = "user@example.com";
        String token = "abc-123-token";

        emailService.sendVerificationEmail(to, token);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getTo()).containsExactly(to);
        assertThat(sentMessage.getSubject()).contains("Email Verification");
        assertThat(sentMessage.getText()).contains(token);
        assertThat(sentMessage.getFrom()).isEqualTo(fromEmail);
    }

    @Test
    @DisplayName("Should send friend request email with correct details")
    void testSendFriendRequestEmail() {
        String to = "friend@example.com";
        String sender = "alice";

        emailService.sendFriendRequestEmail(to, sender);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getTo()).containsExactly(to);
        assertThat(sentMessage.getSubject()).contains("New Friend Request");
        assertThat(sentMessage.getText()).contains(sender);
        assertThat(sentMessage.getFrom()).isEqualTo(fromEmail);
    }

    @Test
    @DisplayName("Should send document share email with correct details")
    void testSendDocumentShareEmail() {
        String to = "recipient@example.com";
        String sender = "bob";
        String docName = "secret_report.pdf";

        emailService.sendDocumentShareEmail(to, sender, docName);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getTo()).containsExactly(to);
        assertThat(sentMessage.getSubject()).contains("Document Shared");
        assertThat(sentMessage.getText()).contains(sender);
        assertThat(sentMessage.getText()).contains(docName);
        assertThat(sentMessage.getFrom()).isEqualTo(fromEmail);
    }
}
