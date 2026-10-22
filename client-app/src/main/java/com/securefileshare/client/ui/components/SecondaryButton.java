package com.securefileshare.client.ui.components;

import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Secondary action button adhering to the Design System.
 * WHAT: Custom Swing JButton with white surface, primary teal border, teal text, and >= 44px height.
 * WHY: Provides standard secondary actions (cancel, back, alternate choices) meeting design specifications.
 */
public class SecondaryButton extends JButton {

    /**
     * Constructs a SecondaryButton with specified label text.
     *
     * @param text Button label text
     */
    public SecondaryButton(String text) {
        super(text);
        setFont(Theme.FONT_BODY_BOLD);
        setForeground(Theme.COLOR_PRIMARY_TEAL);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 20, 10, 20));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, Theme.BUTTON_MIN_HEIGHT);
        return d;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension d = super.getMinimumSize();
        d.height = Theme.BUTTON_MIN_HEIGHT;
        return d;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill background (light teal tint on hover, white default)
        if (getModel().isPressed()) {
            g2.setColor(new Color(0xC7, 0xE7, 0xE9));
        } else if (getModel().isRollover()) {
            g2.setColor(Theme.COLOR_LIGHT_TEAL);
        } else {
            g2.setColor(Theme.COLOR_CARD_WHITE);
        }
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.CARD_CORNER_RADIUS, Theme.CARD_CORNER_RADIUS);

        // Draw 1px primary teal outline border
        g2.setColor(Theme.COLOR_PRIMARY_TEAL);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.CARD_CORNER_RADIUS, Theme.CARD_CORNER_RADIUS);
        g2.dispose();

        super.paintComponent(g);
    }
}
