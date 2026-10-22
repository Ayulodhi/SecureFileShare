package com.securefileshare.client.ui;

import com.securefileshare.client.controller.NavigationService;
import com.securefileshare.client.controller.ReceiveController;
import com.securefileshare.client.model.CheckState;
import com.securefileshare.client.model.ReceiveCheck;
import com.securefileshare.client.model.ReceivedPackage;
import com.securefileshare.client.ui.components.*;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;

/**
 * Screen "Receive a file".
 * WHAT: Dedicated screen providing a "Share link" text field with "Open link" button,
 *       a card listing the five zero-knowledge security checks with real-time status chips,
 *       and action controls ("Save file" and "Discard").
 * WHY: Strict compliance with Phase C5: Decrypts only in an isolated temporary location,
 *      halts immediately on any verification failure, keeps Save button disabled until all 5 pass,
 *      and provides clean file saving via JFileChooser.
 */
public class ReceivePanel extends JPanel implements ReceiveController.ReceiveListener {

    private final ReceiveController receiveController;
    private final NavigationService navigationService;

    private JTextField linkField;
    private PrimaryButton openLinkButton;
    private PrimaryButton saveFileButton;
    private SecondaryButton discardButton;

    private final Map<ReceiveCheck, JPanel> chipSlots = new EnumMap<>(ReceiveCheck.class);
    private JPanel failureBanner;
    private JLabel failureReasonLabel;
    private JPanel fileInfoBox;
    private JLabel fileDetailsLabel;

    private ReceivedPackage verifiedPackage;

    public ReceivePanel(ReceiveController receiveController, NavigationService navigationService) {
        this.receiveController = receiveController;
        this.navigationService = navigationService;

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.COLOR_APP_GROUND);
        setBorder(new EmptyBorder(24, 32, 24, 32));

        initializeUi();
    }

    private void initializeUi() {
        // 1. Header Title
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        PageTitle pageTitle = new PageTitle("Receive a file", "Zero-knowledge verification, cryptographic key unwrapping, and plaintext re-scanning");
        headerPanel.add(pageTitle);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Central Scrollable Content
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);

        // Input Card: Share link field + "Open link" primary button
        Card inputCard = new Card();
        inputCard.setLayout(new BorderLayout(12, 0));
        inputCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JPanel linkInputBox = new JPanel();
        linkInputBox.setLayout(new BoxLayout(linkInputBox, BoxLayout.Y_AXIS));
        linkInputBox.setOpaque(false);

        JLabel linkLabel = new JLabel("Share link:");
        linkLabel.setFont(Theme.FONT_LABEL);
        linkLabel.setForeground(Theme.COLOR_INK_NAVY);

        linkField = Theme.createTextField(25);
        linkField.setText("https://securefileshare.io/share/sample-valid-contract");
        linkField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        linkInputBox.add(linkLabel);
        linkInputBox.add(Box.createVerticalStrut(4));
        linkInputBox.add(linkField);

        openLinkButton = new PrimaryButton("Open link");
        openLinkButton.setPreferredSize(new Dimension(120, Theme.BUTTON_MIN_HEIGHT));
        openLinkButton.addActionListener(e -> startVerification());

        inputCard.add(linkInputBox, BorderLayout.CENTER);
        inputCard.add(openLinkButton, BorderLayout.EAST);
        inputCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        centerContent.add(inputCard);
        centerContent.add(Box.createVerticalStrut(14));

        // File Details Banner (shown when package metadata is fetched)
        fileInfoBox = new JPanel(new BorderLayout(10, 0));
        fileInfoBox.setBackground(Theme.COLOR_LIGHT_TEAL);
        fileInfoBox.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.COLOR_PRIMARY_TEAL, 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));
        fileInfoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        fileInfoBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        fileInfoBox.setVisible(false);

        JLabel docIcon = new JLabel("📄");
        docIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        fileDetailsLabel = new JLabel("Incoming Encrypted Package: —");
        fileDetailsLabel.setFont(Theme.FONT_BODY_BOLD);
        fileDetailsLabel.setForeground(Theme.COLOR_DARK_TEAL_TEXT);

        fileInfoBox.add(docIcon, BorderLayout.WEST);
        fileInfoBox.add(fileDetailsLabel, BorderLayout.CENTER);

        centerContent.add(fileInfoBox);
        centerContent.add(Box.createVerticalStrut(14));

        // 3. Five Security Checks Card
        Card checksCard = new Card();
        checksCard.setLayout(new BoxLayout(checksCard, BoxLayout.Y_AXIS));
        checksCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel checksHeader = new JLabel("Zero-Knowledge Security Verification Pipeline");
        checksHeader.setFont(Theme.FONT_SECTION_HEADER);
        checksHeader.setForeground(Theme.COLOR_INK_NAVY);
        checksCard.add(checksHeader);
        checksCard.add(Box.createVerticalStrut(12));
        checksCard.add(new JSeparator());
        checksCard.add(Box.createVerticalStrut(12));

        for (ReceiveCheck check : ReceiveCheck.values()) {
            JPanel row = createCheckRow(check);
            checksCard.add(row);
            if (check != ReceiveCheck.CHECK_5_MALWARE_RESCAN) {
                checksCard.add(Box.createVerticalStrut(6));
                checksCard.add(new JSeparator());
                checksCard.add(Box.createVerticalStrut(6));
            }
        }

        centerContent.add(checksCard);
        centerContent.add(Box.createVerticalStrut(14));

        // 4. Failure Message Banner (Hidden by default, shown on any check failure)
        failureBanner = new JPanel(new BorderLayout(10, 0));
        failureBanner.setBackground(Theme.CHIP_ERROR_BG);
        failureBanner.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.CHIP_ERROR_TEXT, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));
        failureBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        failureBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        failureBanner.setVisible(false);

        JLabel alertIcon = new JLabel("⚠️");
        alertIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));

        failureReasonLabel = new JLabel("Verification failed");
        failureReasonLabel.setFont(Theme.FONT_BODY);
        failureReasonLabel.setForeground(Theme.CHIP_ERROR_TEXT);

        failureBanner.add(alertIcon, BorderLayout.WEST);
        failureBanner.add(failureReasonLabel, BorderLayout.CENTER);

        centerContent.add(failureBanner);
        centerContent.add(Box.createVerticalStrut(16));

        // 5. Action Buttons below: Primary "Save file", Secondary "Discard"
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        saveFileButton = new PrimaryButton("Save file");
        saveFileButton.setPreferredSize(new Dimension(130, Theme.BUTTON_MIN_HEIGHT));
        saveFileButton.setEnabled(false); // Rule: Stays disabled until all 5 checks pass!
        saveFileButton.addActionListener(e -> promptSaveFile());

        discardButton = new SecondaryButton("Discard");
        discardButton.setPreferredSize(new Dimension(110, Theme.BUTTON_MIN_HEIGHT));
        discardButton.addActionListener(e -> discardReceivedFile());

        actionRow.add(saveFileButton);
        actionRow.add(discardButton);

        centerContent.add(actionRow);
        add(centerContent, BorderLayout.CENTER);
    }

    private JPanel createCheckRow(ReceiveCheck check) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel checkLabel = new JLabel(check.getTitle());
        checkLabel.setFont(Theme.FONT_BODY);
        checkLabel.setForeground(Theme.COLOR_INK_NAVY);

        JPanel chipSlot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        chipSlot.setOpaque(false);
        chipSlots.put(check, chipSlot);

        // Default: Waiting
        renderWaiting(chipSlot);

        row.add(checkLabel, BorderLayout.WEST);
        row.add(chipSlot, BorderLayout.EAST);
        return row;
    }

    private void renderWaiting(JPanel slot) {
        slot.removeAll();
        JLabel l = new JLabel("Waiting");
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.COLOR_MUTED_TEXT);
        slot.add(l);
        slot.revalidate();
        slot.repaint();
    }

    private void startVerification() {
        failureBanner.setVisible(false);
        fileInfoBox.setVisible(false);
        saveFileButton.setEnabled(false);
        openLinkButton.setEnabled(false);
        verifiedPackage = null;

        for (ReceiveCheck check : ReceiveCheck.values()) {
            renderWaiting(chipSlots.get(check));
        }

        String link = linkField.getText();
        receiveController.startReceiveFlow(link, this);
    }

    @Override
    public void onCheckUpdated(ReceiveCheck check, CheckState state) {
        JPanel slot = chipSlots.get(check);
        if (slot == null) return;

        slot.removeAll();
        switch (state.status()) {
            case WAITING -> renderWaiting(slot);
            case IN_PROGRESS -> slot.add(new StatusChip("In progress", StatusChip.Variant.WARNING));
            case PASSED -> slot.add(new StatusChip(state.chipLabel(), StatusChip.Variant.OK));
            case FAILED -> slot.add(new StatusChip(state.chipLabel(), StatusChip.Variant.ERROR));
        }
        slot.revalidate();
        slot.repaint();
    }

    @Override
    public void onPackageLoaded(ReceivedPackage pkg) {
        fileDetailsLabel.setText("Package: " + pkg.getFileName() + " (" + formatSize(pkg.getFileSizeBytes()) + ") • Sent by: " + pkg.getSenderUsername());
        fileInfoBox.setVisible(true);
        revalidate();
        repaint();
    }

    @Override
    public void onAllChecksPassed(ReceivedPackage pkg) {
        this.verifiedPackage = pkg;
        openLinkButton.setEnabled(true);
        saveFileButton.setEnabled(true); // Rule: All 5 checks passed, enable Save file button

        JOptionPane.showMessageDialog(
                this,
                "All 5 zero-knowledge security and malware checks PASSED!\n" +
                        "File '" + pkg.getFileName() + "' is verified, clean, and ready to be saved.",
                "Verification Succeeded",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    @Override
    public void onCheckFailed(ReceiveCheck failedCheck, String reason) {
        openLinkButton.setEnabled(true);
        saveFileButton.setEnabled(false); // Rule: Keep Save button disabled on failure

        failureReasonLabel.setText("<html><b>" + failedCheck.getTitle() + " failed:</b> " + reason + "</html>");
        failureBanner.setVisible(true);
        revalidate();
        repaint();
    }

    @Override
    public void onCancelled() {
        openLinkButton.setEnabled(true);
        saveFileButton.setEnabled(false);
        for (ReceiveCheck check : ReceiveCheck.values()) {
            renderWaiting(chipSlots.get(check));
        }
        failureBanner.setVisible(false);
        fileInfoBox.setVisible(false);
    }

    @Override
    public void onError(String errorMessage) {
        openLinkButton.setEnabled(true);
        saveFileButton.setEnabled(false);
        JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Prompts the user with JFileChooser to save the verified decrypted file.
     * Rule: Save file opens a file chooser and writes the verified file.
     */
    private void promptSaveFile() {
        if (verifiedPackage == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Verified File");
        fileChooser.setSelectedFile(new File(verifiedPackage.getFileName()));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File targetFile = fileChooser.getSelectedFile();
            try {
                receiveController.saveVerifiedFile(targetFile);
                JOptionPane.showMessageDialog(
                        this,
                        "File successfully saved to:\n" + targetFile.getAbsolutePath(),
                        "File Saved",
                        JOptionPane.INFORMATION_MESSAGE
                );
                // Reset state
                discardReceivedFile();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to save file: " + ex.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /**
     * Discards current package, deletes temporary files, and resets the panel.
     */
    private void discardReceivedFile() {
        receiveController.discard();
        verifiedPackage = null;
        saveFileButton.setEnabled(false);
        openLinkButton.setEnabled(true);
        failureBanner.setVisible(false);
        fileInfoBox.setVisible(false);

        for (ReceiveCheck check : ReceiveCheck.values()) {
            renderWaiting(chipSlots.get(check));
        }
        revalidate();
        repaint();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }
}
