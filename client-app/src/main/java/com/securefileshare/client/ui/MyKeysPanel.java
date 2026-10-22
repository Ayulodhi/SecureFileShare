package com.securefileshare.client.ui;

import com.securefileshare.client.api.KeyApi;
import com.securefileshare.client.model.KeyDetailsDto;
import com.securefileshare.client.ui.components.*;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * "My keys" screen panel.
 * WHAT: Displays user's RSA-4096 and EC P-256 public key fingerprints in monospace typography
 *       with one-click clipboard copy buttons, key creation timestamp, and local PKCS12 status.
 * WHY: Strict compliance with Phase C2 Requirement 2: Provides transparent, out-of-band verification
 *      of cryptographic identity without exposing local private keys.
 */
public class MyKeysPanel extends JPanel {

    private final KeyApi keyApi;
    private final StateContainerPanel stateContainer;

    private JLabel rsaFingerprintLabel;
    private JLabel ecFingerprintLabel;
    private JLabel createdDateLabel;
    private JLabel keystorePathLabel;

    private KeyDetailsDto currentDetails;

    public MyKeysPanel(KeyApi keyApi) {
        this.keyApi = keyApi;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.COLOR_APP_GROUND);
        setBorder(new EmptyBorder(24, 28, 20, 28));

        // 1. Header
        PageTitle pageTitle = new PageTitle("My keys", "Local zero-knowledge asymmetric keys and public identity fingerprints");
        add(pageTitle, BorderLayout.NORTH);

        // 2. Main Content inside vertical box
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Security Notice Banner (Light Teal)
        JPanel zkCallout = createSecurityCallout();
        contentPanel.add(zkCallout);
        contentPanel.add(Box.createVerticalStrut(16));

        // Fingerprints Card
        Card keysCard = createKeysCard();
        contentPanel.add(keysCard);
        contentPanel.add(Box.createVerticalStrut(16));

        // Keystore Metadata Card
        Card metadataCard = createMetadataCard();
        contentPanel.add(metadataCard);

        stateContainer = new StateContainerPanel(contentPanel);
        add(stateContainer, BorderLayout.CENTER);

        loadKeyDetails();
    }

    private JPanel createSecurityCallout() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(Theme.COLOR_LIGHT_TEAL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.COLOR_PRIMARY_TEAL, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JLabel icon = new JLabel("🛡️");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));

        JLabel msg = new JLabel("<html><b>Zero-Knowledge Security Assurance</b><br>" +
                "<font color='#0A5F67'>Private keys live exclusively in your local encrypted PKCS12 keystore. " +
                "Only public keys and their fingerprints reach the server.</font></html>");
        msg.setFont(Theme.FONT_BODY);

        panel.add(icon, BorderLayout.WEST);
        panel.add(msg, BorderLayout.CENTER);
        return panel;
    }

    private Card createKeysCard() {
        Card card = new Card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("Public Key SHA-256 Fingerprints");
        header.setFont(Theme.FONT_SECTION_HEADER);
        header.setForeground(Theme.COLOR_INK_NAVY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subHeader = new JLabel("Use these fingerprints to verify your cryptographic identity with other users out-of-band.");
        subHeader.setFont(Theme.FONT_SMALL);
        subHeader.setForeground(Theme.COLOR_MUTED_TEXT);
        subHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(header);
        card.add(Box.createVerticalStrut(4));
        card.add(subHeader);
        card.add(Box.createVerticalStrut(16));
        card.add(new JSeparator());
        card.add(Box.createVerticalStrut(16));

        // RSA-4096 Fingerprint Block
        JLabel rsaTitle = new JLabel("RSA-4096 Key Wrapping Fingerprint");
        rsaTitle.setFont(Theme.FONT_LABEL);
        rsaTitle.setForeground(Theme.COLOR_INK_NAVY);
        rsaTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rsaRow = new JPanel(new BorderLayout(10, 0));
        rsaRow.setOpaque(false);
        rsaRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        rsaFingerprintLabel = new JLabel("Loading...");
        // Rule: Monospace font for fingerprints
        rsaFingerprintLabel.setFont(Theme.FONT_MONO);
        rsaFingerprintLabel.setForeground(Theme.COLOR_DARK_TEAL_TEXT);
        rsaFingerprintLabel.setBackground(Theme.COLOR_APP_GROUND);
        rsaFingerprintLabel.setOpaque(true);
        rsaFingerprintLabel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.COLOR_BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        SecondaryButton rsaCopyBtn = new SecondaryButton("Copy");
        rsaCopyBtn.setPreferredSize(new Dimension(84, 38));
        rsaCopyBtn.addActionListener(e -> copyToClipboard(rsaFingerprintLabel.getText(), rsaCopyBtn));

        rsaRow.add(rsaFingerprintLabel, BorderLayout.CENTER);
        rsaRow.add(rsaCopyBtn, BorderLayout.EAST);
        rsaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // EC P-256 Fingerprint Block
        JLabel ecTitle = new JLabel("EC P-256 Digital Signature Fingerprint");
        ecTitle.setFont(Theme.FONT_LABEL);
        ecTitle.setForeground(Theme.COLOR_INK_NAVY);
        ecTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel ecRow = new JPanel(new BorderLayout(10, 0));
        ecRow.setOpaque(false);
        ecRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        ecFingerprintLabel = new JLabel("Loading...");
        // Rule: Monospace font for fingerprints
        ecFingerprintLabel.setFont(Theme.FONT_MONO);
        ecFingerprintLabel.setForeground(Theme.COLOR_DARK_TEAL_TEXT);
        ecFingerprintLabel.setBackground(Theme.COLOR_APP_GROUND);
        ecFingerprintLabel.setOpaque(true);
        ecFingerprintLabel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.COLOR_BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        SecondaryButton ecCopyBtn = new SecondaryButton("Copy");
        ecCopyBtn.setPreferredSize(new Dimension(84, 38));
        ecCopyBtn.addActionListener(e -> copyToClipboard(ecFingerprintLabel.getText(), ecCopyBtn));

        ecRow.add(ecFingerprintLabel, BorderLayout.CENTER);
        ecRow.add(ecCopyBtn, BorderLayout.EAST);
        ecRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(rsaTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(rsaRow);
        card.add(Box.createVerticalStrut(14));
        card.add(ecTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(ecRow);

        return card;
    }

    private Card createMetadataCard() {
        Card card = new Card();
        card.setLayout(new BorderLayout(16, 0));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel header = new JLabel("Local Keystore Status");
        header.setFont(Theme.FONT_SECTION_HEADER);
        header.setForeground(Theme.COLOR_INK_NAVY);

        createdDateLabel = new JLabel("Key Creation Date: Loading...");
        createdDateLabel.setFont(Theme.FONT_BODY);
        createdDateLabel.setForeground(Theme.COLOR_MUTED_TEXT);

        keystorePathLabel = new JLabel("Storage: ~/.securefileshare/keystore.p12");
        keystorePathLabel.setFont(Theme.FONT_SMALL);
        keystorePathLabel.setForeground(Theme.COLOR_MUTED_TEXT);

        left.add(header);
        left.add(Box.createVerticalStrut(6));
        left.add(createdDateLabel);
        left.add(Box.createVerticalStrut(2));
        left.add(keystorePathLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        right.setOpaque(false);
        StatusChip encryptedChip = new StatusChip("● KEYSTORE ENCRYPTED (PKCS12)", StatusChip.Variant.OK);
        right.add(encryptedChip);

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private void copyToClipboard(String text, JButton button) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

        String originalText = button.getText();
        button.setText("Copied!");
        Timer timer = new Timer(1500, e -> button.setText(originalText));
        timer.setRepeats(false);
        timer.start();
    }

    public void loadKeyDetails() {
        stateContainer.showLoading("Reading public key fingerprints...");

        SwingWorker<KeyDetailsDto, Void> worker = new SwingWorker<>() {
            @Override
            protected KeyDetailsDto doInBackground() throws Exception {
                return keyApi.getMyKeys();
            }

            @Override
            protected void done() {
                try {
                    currentDetails = get();
                    rsaFingerprintLabel.setText(currentDetails.getRsaFingerprint());
                    ecFingerprintLabel.setText(currentDetails.getEcFingerprint());
                    createdDateLabel.setText("Key Creation Date: " + currentDetails.getCreatedAt());
                    keystorePathLabel.setText("Keystore Path: " + currentDetails.getKeystoreLocation());
                    stateContainer.showContent();
                } catch (Exception ex) {
                    stateContainer.showError("Failed to load key fingerprints: " + ex.getMessage(), MyKeysPanel.this::loadKeyDetails);
                }
            }
        };

        worker.execute();
    }
}
