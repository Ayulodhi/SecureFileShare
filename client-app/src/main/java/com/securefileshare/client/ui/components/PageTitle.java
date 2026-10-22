package com.securefileshare.client.ui.components;

import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Standard page title and section header component.
 * WHAT: Renders a primary title in 24px bold ink/navy and an optional subtitle in 14px muted text.
 * WHY: Adheres to Design System typography tokens and eliminates duplicate header styling across screens.
 */
public class PageTitle extends JPanel {

    private final JLabel titleLabel;
    private final JLabel subtitleLabel;

    /**
     * Constructs a PageTitle with title text only.
     *
     * @param title Primary heading text
     */
    public PageTitle(String title) {
        this(title, null);
    }

    /**
     * Constructs a PageTitle with primary title and descriptive subtitle.
     *
     * @param title    Primary heading text
     * @param subtitle Explanatory subtitle text (can be null)
     */
    public PageTitle(String title, String subtitle) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.COLOR_INK_NAVY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(titleLabel);

        if (subtitle != null && !subtitle.trim().isEmpty()) {
            add(Box.createVerticalStrut(4));
            subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(Theme.FONT_SUBTITLE);
            subtitleLabel.setForeground(Theme.COLOR_MUTED_TEXT);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(subtitleLabel);
        } else {
            subtitleLabel = null;
        }
    }

    /**
     * Updates the primary title text.
     *
     * @param title New title string
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    /**
     * Updates the subtitle text if subtitle label is present.
     *
     * @param subtitle New subtitle string
     */
    public void setSubtitle(String subtitle) {
        if (subtitleLabel != null) {
            subtitleLabel.setText(subtitle);
        }
    }
}
