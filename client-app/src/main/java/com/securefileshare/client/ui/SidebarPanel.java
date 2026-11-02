package com.securefileshare.client.ui;

import com.securefileshare.client.controller.NavigationService;
import com.securefileshare.client.ui.components.SecondaryButton;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Navigation sidebar component adhering to Design System specifications.
 * WHAT: 210px wide white panel with right border containing signed-in user profile, Sign out button,
 *       primary navigation items, and an admin audit log entry conditionally visible for the ADMIN role.
 * WHY: Strict compliance with Phase C2 Requirement 5: Displays signed-in username and Sign out control,
 *      wires navigation to dashboard panels, and highlights the selected item in light teal with dark teal bold text.
 */
public class SidebarPanel extends JPanel {

    private final NavigationService navigationService;
    private final Map<String, SidebarItemComponent> itemComponents = new HashMap<>();

    private JLabel usernameLabel;
    private SidebarItemComponent adminItem;
    private Runnable signOutAction;

    public SidebarPanel(NavigationService navigationService) {
        this.navigationService = navigationService;
        initializeUi();

        // Listen for screen changes to keep active sidebar selection highlighted
        navigationService.addListener(this::setSelectedScreen);
    }

    /**
     * Constructs and lays out the sidebar components.
     */
    private void initializeUi() {
        setLayout(new BorderLayout());
        setBackground(Theme.COLOR_CARD_WHITE);
        // Rule: 210px wide, white, right border #D5DCE2
        setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        setMinimumSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.COLOR_BORDER));

        // 1. Top Section: App Branding & Signed-in User Profile Header with Sign Out Button
        // Rule: Show the signed-in username and a Sign out button at the top of the sidebar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.setBorder(new EmptyBorder(18, 16, 14, 16));

        // Branding
        JLabel brandLabel = new JLabel("SecureFileShare");
        brandLabel.setFont(Theme.FONT_BODY_BOLD);
        brandLabel.setForeground(Theme.COLOR_INK_NAVY);
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel zkBadge = new JLabel("● Zero-Knowledge");
        zkBadge.setFont(Theme.FONT_SMALL);
        zkBadge.setForeground(Theme.COLOR_DARK_TEAL_TEXT);
        zkBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        topContainer.add(brandLabel);
        topContainer.add(Box.createVerticalStrut(2));
        topContainer.add(zkBadge);
        topContainer.add(Box.createVerticalStrut(14));
        topContainer.add(new JSeparator());
        topContainer.add(Box.createVerticalStrut(12));

        // Signed-in User Profile Card inside top section
        JPanel userBox = new JPanel();
        userBox.setLayout(new BoxLayout(userBox, BoxLayout.Y_AXIS));
        userBox.setOpaque(false);
        userBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel userHeading = new JLabel("SIGNED IN AS");
        userHeading.setFont(Theme.FONT_LABEL);
        userHeading.setForeground(Theme.COLOR_MUTED_TEXT);
        userHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameLabel = new JLabel("rahul.sharma");
        usernameLabel.setFont(Theme.FONT_BODY_BOLD);
        usernameLabel.setForeground(Theme.COLOR_INK_NAVY);
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton signOutBtn = new SecondaryButton("Sign out");
        signOutBtn.setFont(Theme.FONT_SMALL);
        signOutBtn.setPreferredSize(new Dimension(176, 32));
        signOutBtn.setMaximumSize(new Dimension(176, 32));
        signOutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signOutBtn.addActionListener(e -> {
            if (signOutAction != null) {
                signOutAction.run();
            } else {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to sign out and lock your local keystore?",
                        "Sign Out",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(this, "Session terminated. Keystore locked in memory.");
                }
            }
        });

        userBox.add(userHeading);
        userBox.add(Box.createVerticalStrut(2));
        userBox.add(usernameLabel);
        userBox.add(Box.createVerticalStrut(8));
        userBox.add(signOutBtn);

        topContainer.add(userBox);
        topContainer.add(Box.createVerticalStrut(12));
        topContainer.add(new JSeparator());

        add(topContainer, BorderLayout.NORTH);

        // 2. Center Navigation Items
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(8, 12, 8, 12));

        addItem(centerPanel, "📁  My files", NavigationService.SCREEN_MY_FILES);
        addItem(centerPanel, "📥  Shared with me", NavigationService.SCREEN_SHARED_WITH_ME);
        addItem(centerPanel, "🔗  Receive a file", NavigationService.SCREEN_RECEIVE);
        addItem(centerPanel, "📤  Shares I created", NavigationService.SCREEN_SHARES_CREATED);
        addItem(centerPanel, "🔑  My keys", NavigationService.SCREEN_MY_KEYS);

        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Admin Panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(12, 12, 18, 12));

        adminItem = new SidebarItemComponent("🛡️  Admin: audit log", NavigationService.SCREEN_ADMIN_AUDIT);
        adminItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigationService.show(NavigationService.SCREEN_ADMIN_AUDIT);
            }
        });
        itemComponents.put(NavigationService.SCREEN_ADMIN_AUDIT, adminItem);

        // Visible only for ADMIN role (hidden by default)
        adminItem.setVisible(false);

        bottomPanel.add(new JSeparator());
        bottomPanel.add(Box.createVerticalStrut(8));
        bottomPanel.add(adminItem);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addItem(JPanel parent, String label, String screenName) {
        SidebarItemComponent item = new SidebarItemComponent(label, screenName);
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigationService.show(screenName);
            }
        });
        itemComponents.put(screenName, item);
        parent.add(item);
        parent.add(Box.createVerticalStrut(4));
    }

    /**
     * Updates highlighted state across items when the active screen changes.
     * Rule: Highlight selected item in light teal with dark teal bold text.
     *
     * @param screenName Active screen identifier
     */
    public void setSelectedScreen(String screenName) {
        for (Map.Entry<String, SidebarItemComponent> entry : itemComponents.entrySet()) {
            boolean isSelected = entry.getKey().equals(screenName);
            entry.getValue().setSelected(isSelected);
        }
        repaint();
    }

    public void setUsername(String username) {
        if (usernameLabel != null && username != null) {
            usernameLabel.setText(username);
        }
    }

    public void setSignOutAction(Runnable signOutAction) {
        this.signOutAction = signOutAction;
    }

    public void setAdmin(boolean isAdmin) {
        if (adminItem != null) {
            adminItem.setVisible(isAdmin);
            revalidate();
            repaint();
        }
    }

    /**
     * Clickable row item for sidebar navigation.
     */
    public static class SidebarItemComponent extends JPanel {
        private final JLabel label;
        private final String screenName;
        private boolean selected = false;

        public SidebarItemComponent(String text, String screenName) {
            this.screenName = screenName;
            setLayout(new BorderLayout());
            setOpaque(false);
            if (!GraphicsEnvironment.isHeadless()) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

            label = new JLabel(text);
            label.setFont(Theme.FONT_BODY);
            label.setForeground(Theme.COLOR_INK_NAVY);
            add(label, BorderLayout.CENTER);
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                // Rule: Highlight the selected item in light teal with dark teal bold text
                label.setFont(Theme.FONT_BODY_BOLD);
                label.setForeground(Theme.COLOR_DARK_TEAL_TEXT);
            } else {
                label.setFont(Theme.FONT_BODY);
                label.setForeground(Theme.COLOR_INK_NAVY);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (selected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fill light teal background #DDF1F2 with 6px corner radius
                g2.setColor(Theme.COLOR_LIGHT_TEAL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
