package com.securefileshare.client.ui.components;

import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Reusable loading, error, and content state manager container.
 * WHAT: Switches between an indeterminate loading spinner, an inline error card with Retry button,
 *       and the active content component.
 * WHY: Strict compliance with Phase C2 Requirement 4: Guarantees consistent loading and error
 *      UX across all panels with zero thread blocking.
 */
public class StateContainerPanel extends JPanel {

    private static final String CARD_LOADING = "LOADING";
    private static final String CARD_ERROR = "ERROR";
    private static final String CARD_CONTENT = "CONTENT";

    private final CardLayout cardLayout;
    private final JPanel cardsPanel;

    private final JLabel loadingLabel;
    private final JLabel errorLabel;
    private final SecondaryButton retryButton;
    private Runnable currentRetryAction;

    public StateContainerPanel(JComponent contentComponent) {
        setLayout(new BorderLayout());
        setOpaque(false);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);

        // 1. Loading View
        JPanel loadingPanel = new JPanel();
        loadingPanel.setLayout(new BoxLayout(loadingPanel, BoxLayout.Y_AXIS));
        loadingPanel.setOpaque(false);
        loadingPanel.setBorder(new EmptyBorder(60, 20, 60, 20));

        JProgressBar spinner = new JProgressBar();
        spinner.setIndeterminate(true);
        spinner.setMaximumSize(new Dimension(240, 6));
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);

        loadingLabel = new JLabel("Loading data...");
        loadingLabel.setFont(Theme.FONT_BODY);
        loadingLabel.setForeground(Theme.COLOR_MUTED_TEXT);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loadingPanel.add(spinner);
        loadingPanel.add(Box.createVerticalStrut(14));
        loadingPanel.add(loadingLabel);

        // 2. Error View with Retry Button
        JPanel errorWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 40));
        errorWrapper.setOpaque(false);

        Card errorCard = new Card();
        errorCard.setLayout(new BoxLayout(errorCard, BoxLayout.Y_AXIS));
        errorCard.setPreferredSize(new Dimension(440, 160));

        JLabel errIcon = new JLabel("⚠️  Unable to load data");
        errIcon.setFont(Theme.FONT_BODY_BOLD);
        errIcon.setForeground(Theme.CHIP_ERROR_TEXT);
        errIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        errorLabel = new JLabel("A network or service error occurred.");
        errorLabel.setFont(Theme.FONT_BODY);
        errorLabel.setForeground(Theme.COLOR_MUTED_TEXT);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        retryButton = new SecondaryButton("Retry");
        retryButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        retryButton.addActionListener(e -> {
            if (currentRetryAction != null) {
                currentRetryAction.run();
            }
        });

        errorCard.add(errIcon);
        errorCard.add(Box.createVerticalStrut(8));
        errorCard.add(errorLabel);
        errorCard.add(Box.createVerticalStrut(16));
        errorCard.add(retryButton);
        errorWrapper.add(errorCard);

        // 3. Assemble views into CardLayout
        cardsPanel.add(loadingPanel, CARD_LOADING);
        cardsPanel.add(errorWrapper, CARD_ERROR);
        cardsPanel.add(contentComponent, CARD_CONTENT);

        add(cardsPanel, BorderLayout.CENTER);

        // Default to content
        cardLayout.show(cardsPanel, CARD_CONTENT);
    }

    /**
     * Activates the loading state view with a descriptive status message.
     *
     * @param message Text displayed beneath the loading spinner
     */
    public void showLoading(String message) {
        loadingLabel.setText(message != null ? message : "Loading data...");
        cardLayout.show(cardsPanel, CARD_LOADING);
    }

    /**
     * Activates the error state view with message and retry callback.
     *
     * @param errorMessage User-friendly error message
     * @param retryAction  Callback invoked when the user clicks Retry
     */
    public void showError(String errorMessage, Runnable retryAction) {
        this.currentRetryAction = retryAction;
        this.errorLabel.setText("<html><center>" + (errorMessage != null ? errorMessage : "An error occurred.") + "</center></html>");
        cardLayout.show(cardsPanel, CARD_ERROR);
    }

    /**
     * Displays the primary content component.
     */
    public void showContent() {
        cardLayout.show(cardsPanel, CARD_CONTENT);
    }
}
