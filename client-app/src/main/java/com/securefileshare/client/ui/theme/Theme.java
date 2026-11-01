package com.securefileshare.client.ui.theme;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Central design system definitions and FlatLaf theme installer.
 * WHAT: Encapsulates all color tokens, typography, dimensions, and FlatLaf configuration
 *       for the SecureFileShare client application.
 * WHY: Strict compliance with Design System rules: All visual constants live in ONE class,
 *      preventing magic values across panels and ensuring consistent zero-knowledge UI styling.
 */
public final class Theme {

    // Private constructor prevents direct instantiation of static theme utility
    private Theme() {}

    // --- Color Palette Tokens (Matched Exactly from Specification) ---
    public static final Color COLOR_INK_NAVY = new Color(0x12, 0x20, 0x2B);       // #12202B - primary text / header ink
    public static final Color COLOR_PRIMARY_TEAL = new Color(0x0E, 0x7C, 0x86);   // #0E7C86 - primary button / active accents
    public static final Color COLOR_DARK_TEAL_TEXT = new Color(0x0A, 0x5F, 0x67); // #0A5F67 - dark teal readable text
    public static final Color COLOR_LIGHT_TEAL = new Color(0xDD, 0xF1, 0xF2);     // #DDF1F2 - selection / callout ground
    public static final Color COLOR_APP_GROUND = new Color(0xF4, 0xF6, 0xF8);     // #F4F6F8 - main window canvas background
    public static final Color COLOR_CARD_WHITE = Color.WHITE;                      // #FFFFFF - white card surface background
    public static final Color COLOR_BORDER = new Color(0xD5, 0xDC, 0xE2);         // #D5DCE2 - 1px structural borders
    public static final Color COLOR_MUTED_TEXT = new Color(0x51, 0x60, 0x6B);     // #51606B - secondary / subtitle text

    // --- Status Chip Color Tokens (Background & Text) ---
    public static final Color CHIP_SUCCESS_BG = new Color(0xDD, 0xF1, 0xE0);      // #DDF1E0 - success chip background
    public static final Color CHIP_SUCCESS_TEXT = new Color(0x1F, 0x6B, 0x2A);    // #1F6B2A - success chip text
    public static final Color CHIP_WARNING_BG = new Color(0xFB, 0xEB, 0xCB);      // #FBEBCB - warning chip background
    public static final Color CHIP_WARNING_TEXT = new Color(0x7A, 0x53, 0x00);    // #7A5300 - warning chip text
    public static final Color CHIP_ERROR_BG = new Color(0xF8, 0xDA, 0xD6);        // #F8DAD6 - error chip background
    public static final Color CHIP_ERROR_TEXT = new Color(0x9B, 0x2C, 0x20);      // #9B2C20 - error chip text

    // --- Typography (Segoe UI with System Default Fallback) ---
    public static final String FONT_FAMILY = "Segoe UI";
    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font FONT_SECTION_HEADER = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font FONT_SUBTITLE = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_LABEL = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    // --- Standard Spacing & Sizing Tokens ---
    public static final int BUTTON_MIN_HEIGHT = 44; // Rule: Buttons at least 44px tall
    public static final int CARD_CORNER_RADIUS = 8; // Rule: 8px corner radius
    public static final int CARD_PADDING = 16;       // Rule: 16px padding
    public static final int SIDEBAR_WIDTH = 210;     // Rule: 210px wide sidebar

    /**
     * Installs FlatLaf Look-and-Feel and configures theme colors across Swing defaults.
     * WHAT: Initializes FlatLightLaf and injects application palette into UIManager.
     * WHY: Guarantees clean native anti-aliasing and consistent design system tokens globally.
     */
    public static void install() {
        try {
            FlatLightLaf.setup();

            // Set global component defaults matching theme palette
            UIManager.put("Panel.background", COLOR_APP_GROUND);
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("Focus.color", COLOR_PRIMARY_TEAL);
        } catch (Exception ex) {
            System.err.println("Warning: FlatLaf setup encountered an issue: " + ex.getMessage());
        }
    }

    public static JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(FONT_BODY);
        field.setForeground(COLOR_INK_NAVY);
        field.setBackground(COLOR_CARD_WHITE);
        return field;
    }
}
