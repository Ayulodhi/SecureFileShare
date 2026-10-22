package com.securefileshare.client;

import com.securefileshare.client.config.AppConfig;
import com.securefileshare.client.controller.NavigationService;
import com.securefileshare.client.ui.MainFrame;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;

/**
 * Main desktop application launcher for SecureFileShare.
 * WHAT: Boots the desktop app shell, installs FlatLaf with custom theme tokens,
 *       and displays the MainFrame on the Swing Event Dispatch Thread (EDT).
 * WHY: Entry point meeting the desktop project specifications and threading rules.
 */
public class ClientApplication {

    /**
     * Desktop application entry point.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        // Step 1: Initialize application configuration
        AppConfig config = AppConfig.getInstance();
        System.out.println("Starting SecureFileShare Client [Mode: " + config.getMode() + "]");

        // Step 2: Install FlatLaf Look-and-Feel and custom palette defaults
        Theme.install();

        // Step 3: Launch main frame on the Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                NavigationService navigationService = new NavigationService();
                MainFrame frame = new MainFrame(navigationService);
                frame.setVisible(true);
            } catch (Exception ex) {
                System.err.println("Fatal error starting SecureFileShare client: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }
}
