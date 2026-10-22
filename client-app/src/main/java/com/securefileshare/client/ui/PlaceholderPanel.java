package com.securefileshare.client.ui;

import com.securefileshare.client.ui.components.*;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Placeholder view for application screens in Phase C0.
 * WHAT: Renders a PageTitle, a Card panel, and examples of reusable design system components
 *       (PrimaryButton, SecondaryButton, StatusChip OK/WARNING/ERROR).
 * WHY: Adheres to Phase C0 requirement: "No real screens yet, only placeholders".
 *      Verifies layout, design system styling, and navigation switching.
 */
public class PlaceholderPanel extends JPanel {

    /**
     * Constructs a placeholder panel with the given title and descriptive subtitle.
     *
     * @param title    Page title text
     * @param subtitle Explanatory description
     */
    public PlaceholderPanel(String title, String subtitle) {
        setLayout(new BorderLayout(0, 20));
        setBackground(Theme.COLOR_APP_GROUND);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // Page Header
        PageTitle pageTitle = new PageTitle(title, subtitle);
        add(pageTitle, BorderLayout.NORTH);

        // Center Card Panel displaying Phase C0 Placeholder details
        Card contentCard = new Card();
        contentCard.setLayout(new BoxLayout(contentCard, BoxLayout.Y_AXIS));

        JLabel infoHeader = new JLabel("Phase C0 - App Shell Placeholder");
        infoHeader.setFont(Theme.FONT_BODY_BOLD);
        infoHeader.setForeground(Theme.COLOR_INK_NAVY);
        infoHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel infoDesc = new JLabel("<html>This screen is a placeholder in Phase C0. Screen layout, CardLayout navigation,<br>" +
                "and all reusable design system components are verified below. Real business logic will be added in subsequent phases.</html>");
        infoDesc.setFont(Theme.FONT_BODY);
        infoDesc.setForeground(Theme.COLOR_MUTED_TEXT);
        infoDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Status Chips Demonstrating OK, WARNING, ERROR variants
        JPanel chipsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chipsRow.setOpaque(false);
        chipsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        StatusChip okChip = new StatusChip("● ZERO-KNOWLEDGE: OK", StatusChip.Variant.OK);
        StatusChip warnChip = new StatusChip("▲ PENDING ENCRYPTION", StatusChip.Variant.WARNING);
        StatusChip errChip = new StatusChip("✖ INTEGRITY TAMPERED", StatusChip.Variant.ERROR);

        chipsRow.add(okChip);
        chipsRow.add(warnChip);
        chipsRow.add(errChip);

        // Buttons demonstrating PrimaryButton and SecondaryButton components
        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonsRow.setOpaque(false);
        buttonsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        PrimaryButton primaryBtn = new PrimaryButton("Primary Action");
        SecondaryButton secondaryBtn = new SecondaryButton("Secondary Action");

        buttonsRow.add(primaryBtn);
        buttonsRow.add(secondaryBtn);

        // Assemble Card Content
        contentCard.add(infoHeader);
        contentCard.add(Box.createVerticalStrut(8));
        contentCard.add(infoDesc);
        contentCard.add(Box.createVerticalStrut(20));
        contentCard.add(new JSeparator());
        contentCard.add(Box.createVerticalStrut(16));
        contentCard.add(chipsRow);
        contentCard.add(Box.createVerticalStrut(20));
        contentCard.add(buttonsRow);

        add(contentCard, BorderLayout.CENTER);
    }
}
