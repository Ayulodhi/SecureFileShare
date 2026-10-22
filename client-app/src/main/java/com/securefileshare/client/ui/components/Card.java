package com.securefileshare.client.ui.components;

import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Reusable surface card container component.
 * WHAT: Custom Swing JPanel with white background, 1px border (#D5DCE2), 8px corner radius, and 16px padding.
 * WHY: Strict compliance with Design System card specification. Groups related content into distinct visual blocks.
 */
public class Card extends JPanel {

    /**
     * Constructs a Card with default 16px padding and opaque rendering.
     */
    public Card() {
        this(new BorderLayout());
    }

    /**
     * Constructs a Card with a specified LayoutManager.
     *
     * @param layout Layout manager for child elements
     */
    public Card(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(Theme.CARD_PADDING, Theme.CARD_PADDING, Theme.CARD_PADDING, Theme.CARD_PADDING));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill card background with solid white
        g2.setColor(Theme.COLOR_CARD_WHITE);
        g2.fill(new RoundRectangle2D.Float(
                0, 0, getWidth() - 1, getHeight() - 1, Theme.CARD_CORNER_RADIUS, Theme.CARD_CORNER_RADIUS
        ));

        // Draw 1px structural outline border #D5DCE2
        g2.setColor(Theme.COLOR_BORDER);
        g2.draw(new RoundRectangle2D.Float(
                0, 0, getWidth() - 1, getHeight() - 1, Theme.CARD_CORNER_RADIUS, Theme.CARD_CORNER_RADIUS
        ));

        g2.dispose();
        super.paintComponent(g);
    }
}
