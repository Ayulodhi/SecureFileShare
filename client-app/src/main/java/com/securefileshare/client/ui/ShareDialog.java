package com.securefileshare.client.ui;

import com.securefileshare.client.controller.ShareController;
import com.securefileshare.client.model.FileDto;
import com.securefileshare.client.model.ShareLinkResult;
import com.securefileshare.client.ui.components.Card;
import com.securefileshare.client.ui.components.PrimaryButton;
import com.securefileshare.client.ui.components.SecondaryButton;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;

/**
 * Modal dialog for sharing zero-knowledge encrypted files.
 * WHAT: 760x520 modal dialog centered on the main window, with searchable recipient field,
 *       monospace key fingerprint confirmation card, TOFU key-swap detection, expiry selector,
 *       and validated "Create link" action.
 * WHY: Strict compliance with Phase C4: Defends against server key-substitution attacks by enforcing
 *      explicit sender confirmation and warning if a recipient's public key changes.
 */
public class ShareDialog extends JDialog {

    private final ShareController shareController;
    private final FileDto fileToShare;
    private final Runnable onShareCreated;

    private JComboBox<String> recipientField;
    private JLabel fingerprintLabel;
    private JLabel confirmNoteLabel;
    private JCheckBox confirmCheckbox;
    private JComboBox<String> expiryDropdown;
    private PrimaryButton createLinkButton;
    private SecondaryButton cancelButton;
    private JLabel footerNoteLabel;

    private CardLayout mainLayout;
    private JPanel containerPanel;
    private JTextField linkTextField;
    private JLabel expiryTimeLabel;
    private SecondaryButton copyLinkButton;

    private static final String CARD_FORM = "FORM";
    private static final String CARD_SUCCESS = "SUCCESS";

    private static final String[] SAMPLE_RECIPIENTS = {
            "rahul",
            "priya",
            "aman",
            "neha",
            "dev-team"
    };

    private static final String[] EXPIRY_OPTIONS = {
            "1 hour",
            "24 hours",
            "7 days",
            "30 days"
    };

    public ShareDialog(Frame owner, ShareController shareController, FileDto fileToShare, Runnable onShareCreated) {
        // Rule: 760x520, modal, centered on main window, title "Share securely"
        super(owner, "Share securely", true);
        this.shareController = shareController;
        this.fileToShare = fileToShare;
        this.onShareCreated = onShareCreated;

        setSize(760, 520);
        setResizable(false);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(Theme.COLOR_APP_GROUND);

        mainLayout = new CardLayout();
        containerPanel = new JPanel(mainLayout);
        containerPanel.setOpaque(false);

        // Build Form view and Success view
        JPanel formView = buildFormView();
        JPanel successView = buildSuccessView();

        containerPanel.add(formView, CARD_FORM);
        containerPanel.add(successView, CARD_SUCCESS);

        add(containerPanel);

        // Initial recipient resolution
        updateRecipientState();
    }

    private JPanel buildFormView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 28, 20, 28));

        // 1. Header
        JLabel title = new JLabel("Share securely");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.COLOR_INK_NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        String fileInfo = (fileToShare != null) ? fileToShare.getName() + " (" + fileToShare.getFormattedSize() + ")" : "Encrypted Document";
        JLabel subtitle = new JLabel("File: " + fileInfo + " • Zero-knowledge encryption key will be wrapped for recipient");
        subtitle.setFont(Theme.FONT_SUBTITLE);
        subtitle.setForeground(Theme.COLOR_MUTED_TEXT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(2));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(14));

        // 2. Recipient field (Searchable)
        JLabel recipientHeading = new JLabel("Recipient username:");
        recipientHeading.setFont(Theme.FONT_LABEL);
        recipientHeading.setForeground(Theme.COLOR_INK_NAVY);
        recipientHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        recipientField = new JComboBox<>(SAMPLE_RECIPIENTS);
        recipientField.setEditable(true);
        recipientField.setFont(Theme.FONT_BODY);
        recipientField.setBackground(Color.WHITE);
        recipientField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        recipientField.setPreferredSize(new Dimension(Integer.MAX_VALUE, 38));
        recipientField.setAlignmentX(Component.LEFT_ALIGNMENT);

        recipientField.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateRecipientState();
            }
        });

        // Also listen to text editor changes
        JTextField editor = (JTextField) recipientField.getEditor().getEditorComponent();
        editor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                updateRecipientState();
            }
        });

        panel.add(recipientHeading);
        panel.add(Box.createVerticalStrut(4));
        panel.add(recipientField);
        panel.add(Box.createVerticalStrut(12));

        // 3. Light card titled "Recipient key fingerprint"
        JPanel fingerprintCard = new JPanel();
        fingerprintCard.setLayout(new BoxLayout(fingerprintCard, BoxLayout.Y_AXIS));
        fingerprintCard.setBackground(Theme.COLOR_LIGHT_TEAL);
        fingerprintCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.COLOR_PRIMARY_TEAL, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));
        fingerprintCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        fingerprintCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel fpCardTitle = new JLabel("Recipient key fingerprint");
        fpCardTitle.setFont(Theme.FONT_LABEL);
        fpCardTitle.setForeground(Theme.COLOR_DARK_TEAL_TEXT);
        fpCardTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Monospace font fingerprint
        fingerprintLabel = new JLabel("A3:5F:0C:9E:21:7B:D4:88");
        fingerprintLabel.setFont(Theme.FONT_MONO);
        fingerprintLabel.setForeground(Theme.COLOR_DARK_TEAL_TEXT);
        fingerprintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        confirmNoteLabel = new JLabel("Confirm this with rahul by phone or in person. It protects you from a swapped key.");
        confirmNoteLabel.setFont(Theme.FONT_SMALL);
        confirmNoteLabel.setForeground(Theme.COLOR_DARK_TEAL_TEXT);
        confirmNoteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Checkbox: "I have confirmed this fingerprint"
        confirmCheckbox = new JCheckBox("I have confirmed this fingerprint with the recipient");
        confirmCheckbox.setFont(Theme.FONT_BODY);
        confirmCheckbox.setForeground(Theme.COLOR_INK_NAVY);
        confirmCheckbox.setOpaque(false);
        confirmCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmCheckbox.addActionListener(e -> updateButtonState());

        fingerprintCard.add(fpCardTitle);
        fingerprintCard.add(Box.createVerticalStrut(4));
        fingerprintCard.add(fingerprintLabel);
        fingerprintCard.add(Box.createVerticalStrut(6));
        fingerprintCard.add(confirmNoteLabel);
        fingerprintCard.add(Box.createVerticalStrut(6));
        fingerprintCard.add(confirmCheckbox);

        panel.add(fingerprintCard);
        panel.add(Box.createVerticalStrut(12));

        // 4. "Link expires after" dropdown
        JPanel expiryRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        expiryRow.setOpaque(false);
        expiryRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel expiryLabel = new JLabel("Link expires after:");
        expiryLabel.setFont(Theme.FONT_LABEL);
        expiryLabel.setForeground(Theme.COLOR_INK_NAVY);

        expiryDropdown = new JComboBox<>(EXPIRY_OPTIONS);
        expiryDropdown.setSelectedItem("7 days");
        expiryDropdown.setFont(Theme.FONT_BODY);
        expiryDropdown.setBackground(Color.WHITE);
        expiryDropdown.setPreferredSize(new Dimension(140, 36));

        expiryRow.add(expiryLabel);
        expiryRow.add(expiryDropdown);

        panel.add(expiryRow);
        panel.add(Box.createVerticalStrut(16));

        // 5. Action Buttons (Primary "Create link", Secondary "Cancel")
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        cancelButton = new SecondaryButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(100, Theme.BUTTON_MIN_HEIGHT));
        cancelButton.addActionListener(e -> dispose());

        createLinkButton = new PrimaryButton("Create link");
        createLinkButton.setPreferredSize(new Dimension(140, Theme.BUTTON_MIN_HEIGHT));
        createLinkButton.setEnabled(false); // Starts disabled per Requirement 2
        createLinkButton.addActionListener(e -> executeCreateLink());

        buttonRow.add(cancelButton);
        buttonRow.add(createLinkButton);

        panel.add(buttonRow);
        panel.add(Box.createVerticalStrut(10));

        // 6. Muted footer
        footerNoteLabel = new JLabel("Only rahul can use this link. You can revoke it at any time.");
        footerNoteLabel.setFont(Theme.FONT_SMALL);
        footerNoteLabel.setForeground(Theme.COLOR_MUTED_TEXT);
        footerNoteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(footerNoteLabel);

        return panel;
    }

    private JPanel buildSuccessView() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(30, 32, 24, 32));

        JLabel successTitle = new JLabel("✓ Secure Link Created");
        successTitle.setFont(Theme.FONT_TITLE);
        successTitle.setForeground(Theme.COLOR_PRIMARY_TEAL);
        successTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel successSubtitle = new JLabel("AES-256 encryption key was wrapped with recipient's public key.");
        successSubtitle.setFont(Theme.FONT_SUBTITLE);
        successSubtitle.setForeground(Theme.COLOR_MUTED_TEXT);
        successSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(successTitle);
        panel.add(Box.createVerticalStrut(4));
        panel.add(successSubtitle);
        panel.add(Box.createVerticalStrut(24));

        // Link Display Card
        Card linkCard = new Card();
        linkCard.setLayout(new BorderLayout(12, 0));
        linkCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

        linkTextField = new JTextField();
        linkTextField.setEditable(false);
        linkTextField.setFont(Theme.FONT_MONO);
        linkTextField.setForeground(Theme.COLOR_INK_NAVY);
        linkTextField.setBackground(Color.WHITE);

        copyLinkButton = new SecondaryButton("Copy");
        copyLinkButton.setPreferredSize(new Dimension(90, 38));
        copyLinkButton.addActionListener(e -> {
            StringSelection sel = new StringSelection(linkTextField.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            copyLinkButton.setText("Copied!");
            Timer t = new Timer(1500, ev -> copyLinkButton.setText("Copy"));
            t.setRepeats(false);
            t.start();
        });

        linkCard.add(linkTextField, BorderLayout.CENTER);
        linkCard.add(copyLinkButton, BorderLayout.EAST);
        linkCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(linkCard);
        panel.add(Box.createVerticalStrut(12));

        expiryTimeLabel = new JLabel("Expires: in 7 days");
        expiryTimeLabel.setFont(Theme.FONT_BODY);
        expiryTimeLabel.setForeground(Theme.COLOR_MUTED_TEXT);
        expiryTimeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(expiryTimeLabel);
        panel.add(Box.createVerticalStrut(24));

        PrimaryButton doneBtn = new PrimaryButton("Done");
        doneBtn.setPreferredSize(new Dimension(120, Theme.BUTTON_MIN_HEIGHT));
        doneBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        doneBtn.addActionListener(e -> dispose());

        panel.add(doneBtn);
        return panel;
    }

    /**
     * Resolves the fingerprint for the chosen recipient and evaluates key-change warnings.
     */
    private void updateRecipientState() {
        String recipient = getSelectedRecipient();
        String fp = shareController.resolveRecipientFingerprint(recipient);

        fingerprintLabel.setText(fp);
        confirmNoteLabel.setText("Confirm this with " + (recipient.isEmpty() ? "<recipient>" : recipient) + " by phone or in person. It protects you from a swapped key.");
        footerNoteLabel.setText("Only " + (recipient.isEmpty() ? "<recipient>" : recipient) + " can use this link. You can revoke it at any time.");

        // Requirement 3: Compare new fingerprint with saved one; show red warning dialog if changed
        if (!recipient.isEmpty() && shareController.isKeyChanged(recipient, fp)) {
            showKeyChangedWarning(recipient, shareController.getSavedFingerprint(recipient), fp);
        }

        updateButtonState();
    }

    /**
     * Updates the enabled state of the Create link button.
     * Rule: Stays disabled until a recipient is chosen AND checkbox is ticked.
     */
    private void updateButtonState() {
        String recipient = getSelectedRecipient();
        boolean confirmed = confirmCheckbox.isSelected();

        // Security Decision: Defends against server key substitution
        // Why: Requiring the sender to explicitly confirm the recipient's fingerprint before key wrapping
        // ensures the sender has verified the public key out-of-band and protects against server MITM key swaps.
        boolean enabled = shareController.isCreateLinkEnabled(recipient, confirmed);
        createLinkButton.setEnabled(enabled);
    }

    private String getSelectedRecipient() {
        Object item = recipientField.getEditor().getItem();
        if (item == null) {
            item = recipientField.getSelectedItem();
        }
        return item != null ? item.toString().trim() : "";
    }

    /**
     * Displays a red warning dialog when a recipient's public key fingerprint has changed.
     */
    private void showKeyChangedWarning(String recipient, String savedFp, String currentFp) {
        String message = String.format("""
                ⚠️ WARNING: POTENTIAL KEY SUBSTITUTION DETECTED!

                The cryptographic key fingerprint for '%s' has CHANGED from what was previously saved on this device.

                Previously trusted: %s
                Received now:       %s

                Why: This may indicate an active Man-In-The-Middle attack by a compromised server attempting to intercept this file.
                Do NOT proceed unless '%s' has personally confirmed they rotated their private keys!
                """, recipient, savedFp, currentFp, recipient);

        JOptionPane.showMessageDialog(
                this,
                message,
                "SECURITY WARNING: Fingerprint Changed",
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Executes the link creation via ShareController in background worker.
     */
    private void executeCreateLink() {
        String recipient = getSelectedRecipient();
        String expiry = (String) expiryDropdown.getSelectedItem();
        String fileId = (fileToShare != null) ? fileToShare.getId() : "file-101";
        String fileName = (fileToShare != null) ? fileToShare.getName() : "document.pdf";

        createLinkButton.setEnabled(false);
        cancelButton.setEnabled(false);

        shareController.createLink(fileId, fileName, recipient, expiry, (result, ex) -> {
            if (ex != null) {
                createLinkButton.setEnabled(true);
                cancelButton.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Failed to create share: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Show generated link and expiry in read-only field (Requirement 4)
            linkTextField.setText(result.getShareUrl());
            expiryTimeLabel.setText(result.getFormattedExpiry());

            // Switch to success card
            mainLayout.show(containerPanel, CARD_SUCCESS);

            // Refresh "Shares I created" list
            if (onShareCreated != null) {
                onShareCreated.run();
            }
        });
    }
}
