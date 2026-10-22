package com.securefileshare.client.ui.components;

import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Reusable status chip label adhering to the Design System.
 * WHAT: Small rounded pill label rendering semantic status indicators (OK, WARNING, ERROR).
 * WHY: Visual indicator for zero-knowledge state, file integrity checks, and audit verifications
 *      without hardcoded magic color values.
 */
public class StatusChip extends JLabel {

    /**
     * Semantic status chip variants defining background and text colors.
     */
    public enum Variant {
        OK(Theme.CHIP_SUCCESS_BG, Theme.CHIP_SUCCESS_TEXT),
        WARNING(Theme.CHIP_WARNING_BG, Theme.CHIP_WARNING_TEXT),
        ERROR(Theme.CHIP_ERROR_BG, Theme.CHIP_ERROR_TEXT);

        private final Color background;
        private final Color text;

        Variant(Color background, Color text) {
            this.background = background;
            this.text = text;
        }

        public Color getBackground() {
            return background;
        }

        public Color getText() {
            return text;
        }
    }

    private Color chipBg;
    private Color chipFg;

    /**
     * Constructs a StatusChip with label text and specified semantic variant.
     *
     * @param text    Display status text
     * @param variant OK, WARNING, or ERROR variant
     */
    public StatusChip(String text, Variant variant) {
        super(text);
        setVariant(variant);
        setFont(Theme.FONT_LABEL);
        setBorder(new EmptyBorder(4, 12, 4, 12));
        setOpaque(false);
    }

    /**
     * Updates the status chip variant dynamically.
     *
     * @param variant New semantic variant
     */
    public void setVariant(Variant variant) {
        this.chipBg = variant.getBackground();
        this.chipFg = variant.getText();
        setForeground(this.chipFg);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw pill background with 12px corner arc
        g2.setColor(chipBg);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
        g2.dispose();

        super.paintComponent(g);
    }
}
