package com.securefileshare.client.ui;

import com.securefileshare.client.controller.NavigationService;
import com.securefileshare.client.controller.UploadController;
import com.securefileshare.client.model.EncryptedPackage;
import com.securefileshare.client.model.ServerVerdict;
import com.securefileshare.client.model.StepStatus;
import com.securefileshare.client.model.UploadStep;
import com.securefileshare.client.ui.components.Card;
import com.securefileshare.client.ui.components.SecondaryButton;
import com.securefileshare.client.ui.components.StatusChip;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;

/**
 * Screen "Uploading <file name>".
 * WHAT: Dedicated screen rendering title, size and recipient subtitle, a card with six numbered steps,
 *       status chips (Done = green, In progress = amber, Waiting = muted text), teal progress bar,
 *       large-file byte counter, light teal security assurance card, and a secondary Cancel button.
 * WHY: Strict compliance with Phase C3 specifications: Visually exhibits the zero-knowledge security
 *      pipeline and guarantees that infected files or cancellations never leak plaintext.
 */
public class UploadProgressPanel extends JPanel implements UploadController.UploadListener {

    private final UploadController uploadController;
    private final NavigationService navigationService;
    private final Runnable onUploadFinished;

    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JProgressBar progressBar;
    private JLabel byteProgressLabel;
    private SecondaryButton cancelButton;

    private final Map<UploadStep, JPanel> stepRowPanels = new EnumMap<>(UploadStep.class);
    private final Map<UploadStep, JPanel> chipSlots = new EnumMap<>(UploadStep.class);

    private File currentFile;
    private String currentRecipient;

    public UploadProgressPanel(UploadController uploadController, NavigationService navigationService, Runnable onUploadFinished) {
        this.uploadController = uploadController;
        this.navigationService = navigationService;
        this.onUploadFinished = onUploadFinished;

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.COLOR_APP_GROUND);
        setBorder(new EmptyBorder(24, 32, 24, 32));

        initializeUi();
    }

    private void initializeUi() {
        // 1. Header (Title + Muted line with size and recipient)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        titleLabel = new JLabel("Uploading file...");
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.COLOR_INK_NAVY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtitleLabel = new JLabel("0 KB • Recipient: -");
        subtitleLabel.setFont(Theme.FONT_SUBTITLE);
        subtitleLabel.setForeground(Theme.COLOR_MUTED_TEXT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Central Content
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);

        // Steps Card
        Card stepsCard = new Card();
        stepsCard.setLayout(new BoxLayout(stepsCard, BoxLayout.Y_AXIS));

        for (UploadStep step : UploadStep.values()) {
            JPanel row = createStepRow(step);
            stepRowPanels.put(step, row);
            stepsCard.add(row);
            if (step != UploadStep.STEP_6_SERVER_VERDICT) {
                stepsCard.add(Box.createVerticalStrut(8));
                stepsCard.add(new JSeparator());
                stepsCard.add(Box.createVerticalStrut(8));
            }
        }

        stepsCard.add(Box.createVerticalStrut(18));

        // Under the steps: Teal progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setForeground(Theme.COLOR_PRIMARY_TEAL);
        progressBar.setBackground(new Color(0xE2, 0xEB, 0xF0));
        progressBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 8));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        progressBar.setBorderPainted(false);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        byteProgressLabel = new JLabel("Preparing upload pipeline...");
        byteProgressLabel.setFont(Theme.FONT_SMALL);
        byteProgressLabel.setForeground(Theme.COLOR_MUTED_TEXT);
        byteProgressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        stepsCard.add(progressBar);
        stepsCard.add(Box.createVerticalStrut(6));
        stepsCard.add(byteProgressLabel);

        centerContent.add(stepsCard);
        centerContent.add(Box.createVerticalStrut(16));

        // Below the card: Light teal notice card with teal border
        JPanel noticeCard = new JPanel(new BorderLayout(12, 0));
        noticeCard.setBackground(Theme.COLOR_LIGHT_TEAL);
        noticeCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.COLOR_PRIMARY_TEAL, 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));
        noticeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel lockIcon = new JLabel("🔒");
        lockIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JLabel noticeText = new JLabel("Your file is never sent unencrypted. This device holds the only keys that can open it.");
        noticeText.setFont(Theme.FONT_BODY);
        noticeText.setForeground(Theme.COLOR_DARK_TEAL_TEXT);

        noticeCard.add(lockIcon, BorderLayout.WEST);
        noticeCard.add(noticeText, BorderLayout.CENTER);
        noticeCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        centerContent.add(noticeCard);
        centerContent.add(Box.createVerticalStrut(16));

        // Secondary Cancel Button
        cancelButton = new SecondaryButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(140, Theme.BUTTON_MIN_HEIGHT));
        cancelButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        cancelButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Cancel this upload? Any temporary encrypted files will be deleted immediately.",
                    "Cancel Upload",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                uploadController.cancel();
            }
        });

        centerContent.add(cancelButton);
        add(centerContent, BorderLayout.CENTER);
    }

    private JPanel createStepRow(UploadStep step) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel stepLabel = new JLabel(step.getFormattedLabel());
        stepLabel.setFont(Theme.FONT_BODY);
        stepLabel.setForeground(Theme.COLOR_INK_NAVY);

        JPanel chipSlot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        chipSlot.setOpaque(false);
        chipSlots.put(step, chipSlot);

        // Default state: Waiting (muted text with no chip)
        renderWaitingState(chipSlot);

        row.add(stepLabel, BorderLayout.WEST);
        row.add(chipSlot, BorderLayout.EAST);
        return row;
    }

    private void renderWaitingState(JPanel slot) {
        slot.removeAll();
        JLabel waitingLabel = new JLabel("Waiting");
        waitingLabel.setFont(Theme.FONT_SMALL);
        waitingLabel.setForeground(Theme.COLOR_MUTED_TEXT);
        slot.add(waitingLabel);
        slot.revalidate();
        slot.repaint();
    }

    /**
     * Initiates the upload flow for a chosen file and recipient.
     *
     * @param file      Local candidate file
     * @param recipient Target username
     */
    public void startUpload(File file, String recipient) {
        this.currentFile = file;
        this.currentRecipient = recipient;

        String sizeFormatted = formatSize(file.length());
        titleLabel.setText("Uploading " + file.getName());
        subtitleLabel.setText(sizeFormatted + " • Recipient: " + recipient);

        progressBar.setValue(0);
        cancelButton.setEnabled(true);

        // Reset all step slots to Waiting
        for (UploadStep step : UploadStep.values()) {
            renderWaitingState(chipSlots.get(step));
        }

        // Run background execution via controller
        uploadController.startUpload(file, recipient, this);
    }

    @Override
    public void onStepChanged(UploadStep step, StepStatus status) {
        JPanel slot = chipSlots.get(step);
        if (slot == null) return;

        slot.removeAll();
        // Rule: Done = green chip, in progress = amber chip "In progress", waiting = muted text with no chip
        switch (status) {
            case DONE -> slot.add(new StatusChip("Done", StatusChip.Variant.OK));
            case IN_PROGRESS -> slot.add(new StatusChip("In progress", StatusChip.Variant.WARNING));
            case WAITING -> renderWaitingState(slot);
            case FAILED -> slot.add(new StatusChip(status.getLabel(), StatusChip.Variant.ERROR));
        }
        slot.revalidate();
        slot.repaint();
    }

    @Override
    public void onByteProgress(long bytesProcessed, long totalBytes, String stepDescription) {
        String processedStr = formatSize(bytesProcessed);
        String totalStr = formatSize(totalBytes);
        int pct = totalBytes > 0 ? (int) ((bytesProcessed * 100) / totalBytes) : 0;
        byteProgressLabel.setText(stepDescription + " [" + processedStr + " / " + totalStr + " (" + pct + "%)]");
    }

    @Override
    public void onOverallProgress(int percent) {
        progressBar.setValue(percent);
    }

    @Override
    public void onCompleted(ServerVerdict verdict, EncryptedPackage pkg) {
        cancelButton.setEnabled(false);

        // Handle outcomes per Requirement 4:
        switch (verdict) {
            case CLEAN -> {
                JOptionPane.showMessageDialog(
                        this,
                        "File '" + currentFile.getName() + "' uploaded and encrypted successfully!\n" +
                                "Payload is securely stored in zero-knowledge object storage.",
                        "Upload Complete",
                        JOptionPane.INFORMATION_MESSAGE
                );
                returnToMyFiles();
            }
            case SUSPICIOUS -> {
                JOptionPane.showMessageDialog(
                        this,
                        "Quarantined for review:\n" +
                                "File heuristics triggered secondary validation. File is stored in quarantined state.\n" +
                                "Sharing has been disabled until reviewed.",
                        "File Quarantined",
                        JOptionPane.WARNING_MESSAGE
                );
                returnToMyFiles();
            }
            case INFECTED -> {
                JOptionPane.showMessageDialog(
                        this,
                        "File Rejected:\n" +
                                "Server integrity inspection detected malicious payload characteristics. Package deleted.",
                        "Upload Rejected",
                        JOptionPane.ERROR_MESSAGE
                );
                returnToMyFiles();
            }
        }
    }

    @Override
    public void onMalwareDetected(String fileName) {
        cancelButton.setEnabled(false);
        // Requirement 4: Scan finds malware (stop at step 1, red alert dialog, nothing uploaded)
        JOptionPane.showMessageDialog(
                this,
                "Threat Detected!\n" +
                        "Local scanner (YARA + ClamAV) flagged '" + fileName + "' as malicious.\n" +
                        "Upload stopped immediately at Step 1. No data was sent over the network.",
                "Malware Detected",
                JOptionPane.ERROR_MESSAGE
        );
        returnToMyFiles();
    }

    @Override
    public void onCancelled() {
        byteProgressLabel.setText("Upload cancelled. Temporary files deleted.");
        JOptionPane.showMessageDialog(
                this,
                "Upload was cancelled. Temporary encrypted files have been purged from disk.",
                "Upload Cancelled",
                JOptionPane.INFORMATION_MESSAGE
        );
        returnToMyFiles();
    }

    @Override
    public void onError(String errorMessage) {
        cancelButton.setEnabled(false);
        JOptionPane.showMessageDialog(
                this,
                "Upload failed: " + errorMessage,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
        returnToMyFiles();
    }

    private void returnToMyFiles() {
        if (onUploadFinished != null) {
            onUploadFinished.run();
        } else {
            navigationService.show(NavigationService.SCREEN_MY_FILES);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }
}
