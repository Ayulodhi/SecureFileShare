package com.securefileshare.client.ui.components;

import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Primary action button adhering to the Design System.
 * WHAT: Custom Swing JButton with filled primary teal background, white text, and >= 44px height.
 * WHY: Standard call-to-action button enforcing minimum touch target height and palette compliance.
 */
public class PrimaryButton extends JButton {

    /**
     * Constructs a PrimaryButton with specified label text.
     *
     * @param text Button label text
     */
    public PrimaryButton(String text) {
        super(text);
        setFont(Theme.FONT_BODY_BOLD);
        setForeground(Color.WHITE);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        if (!GraphicsEnvironment.isHeadless()) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
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

        // Determine background shade based on button interaction state
        if (getModel().isPressed()) {
            g2.setColor(Theme.COLOR_DARK_TEAL_TEXT);
        } else if (getModel().isRollover()) {
            g2.setColor(new Color(0x0B, 0x67, 0x70));
        } else {
            g2.setColor(Theme.COLOR_PRIMARY_TEAL);
        }

        // Draw 8px rounded rectangle background
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.CARD_CORNER_RADIUS, Theme.CARD_CORNER_RADIUS);
        g2.dispose();

        super.paintComponent(g);
    }
}
