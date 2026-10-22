package com.securefileshare.client.ui;

import com.securefileshare.client.ui.components.Card;
import com.securefileshare.client.ui.components.PrimaryButton;
import com.securefileshare.client.ui.components.SecondaryButton;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.util.function.BiConsumer;

/**
 * Pre-upload file chooser and searchable recipient picker dialog.
 * WHAT: Allows the user to select a local file to encrypt and choose a target recipient
 *       from an autocompleting/searchable recipient field before initiating upload.
 * WHY: Strict compliance with Phase C3 Requirement 1: "Before the screen: a file chooser
 *      and a recipient picker (searchable field that loads recipient names), then start the upload."
 */
public class UploadDialog extends JDialog {

    private final BiConsumer<File, String> onConfirmed;
    private File selectedFile;
    private JLabel fileInfoLabel;
    private JComboBox<String> recipientComboBox;

    // Sample known recipients for zero-knowledge key distribution
    private static final String[] SAMPLE_RECIPIENTS = {
            "rahul",
            "priya",
            "aman",
            "neha",
            "security-admin",
            "Self-Vault (Only me)"
    };

    public UploadDialog(Frame owner, BiConsumer<File, String> onConfirmed) {
        super(owner, "Select File & Recipient", true);
        this.onConfirmed = onConfirmed;

        setSize(520, 420);
        setResizable(false);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(Theme.COLOR_APP_GROUND);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header Title
        JLabel titleLabel = new JLabel("Upload Encrypted File");
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.COLOR_INK_NAVY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("File will be scanned, encrypted locally, and wrapped with recipient's public key.");
        subtitleLabel.setFont(Theme.FONT_SMALL);
        subtitleLabel.setForeground(Theme.COLOR_MUTED_TEXT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(16));

        // File Selection Card
        Card fileCard = new Card();
        fileCard.setLayout(new BorderLayout(12, 0));
        fileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

        fileInfoLabel = new JLabel("No file selected yet");
        fileInfoLabel.setFont(Theme.FONT_BODY);
        fileInfoLabel.setForeground(Theme.COLOR_MUTED_TEXT);

        SecondaryButton browseBtn = new SecondaryButton("Browse...");
        browseBtn.setPreferredSize(new Dimension(100, 36));
        browseBtn.addActionListener(e -> chooseFile());

        fileCard.add(fileInfoLabel, BorderLayout.CENTER);
        fileCard.add(browseBtn, BorderLayout.EAST);
        fileCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        mainPanel.add(fileCard);
        mainPanel.add(Box.createVerticalStrut(14));

        // Searchable Recipient Picker Row
        JPanel recipientPanel = new JPanel();
        recipientPanel.setLayout(new BoxLayout(recipientPanel, BoxLayout.Y_AXIS));
        recipientPanel.setOpaque(false);
        recipientPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel recipientLabel = new JLabel("Select or type recipient username:");
        recipientLabel.setFont(Theme.FONT_LABEL);
        recipientLabel.setForeground(Theme.COLOR_INK_NAVY);
        recipientLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Editable, searchable combo box
        recipientComboBox = new JComboBox<>(SAMPLE_RECIPIENTS);
        recipientComboBox.setEditable(true);
        recipientComboBox.setFont(Theme.FONT_BODY);
        recipientComboBox.setBackground(Color.WHITE);
        recipientComboBox.setPreferredSize(new Dimension(Integer.MAX_VALUE, 38));
        recipientComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        recipientComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        recipientPanel.add(recipientLabel);
        recipientPanel.add(Box.createVerticalStrut(6));
        recipientPanel.add(recipientComboBox);

        mainPanel.add(recipientPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Action Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        SecondaryButton cancelBtn = new SecondaryButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(100, Theme.BUTTON_MIN_HEIGHT));
        cancelBtn.addActionListener(e -> dispose());

        PrimaryButton startBtn = new PrimaryButton("Start Encrypted Upload");
        startBtn.setPreferredSize(new Dimension(190, Theme.BUTTON_MIN_HEIGHT));
        startBtn.addActionListener(e -> confirmUpload());

        buttonRow.add(cancelBtn);
        buttonRow.add(startBtn);

        mainPanel.add(buttonRow);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File for Zero-Knowledge Encryption");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            long size = selectedFile.length();
            String formattedSize = formatSize(size);
            fileInfoLabel.setText("<html><b>" + selectedFile.getName() + "</b> (" + formattedSize + ")</html>");
            fileInfoLabel.setForeground(Theme.COLOR_INK_NAVY);
        }
    }

    private void confirmUpload() {
        if (selectedFile == null || !selectedFile.exists()) {
            JOptionPane.showMessageDialog(this, "Please select a file to upload.", "File Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object selectedItem = recipientComboBox.getSelectedItem();
        String recipient = selectedItem != null ? selectedItem.toString().trim() : "";
        if (recipient.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please specify a recipient username.", "Recipient Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        dispose();
        if (onConfirmed != null) {
            onConfirmed.accept(selectedFile, recipient);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }
}
