package com.securefileshare.client.controller;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Screen navigation service decoupling UI views from one another.
 * WHAT: Coordinates CardLayout screen transitions between registered views and notifies listeners.
 * WHY: Architecture rule: Screens never reference each other directly; all navigation requests
 *      route through NavigationService to maintain clean boundaries.
 */
public class NavigationService {

    // Standard named screen identifiers
    public static final String SCREEN_MY_FILES = "MY_FILES";
    public static final String SCREEN_SHARED_WITH_ME = "SHARED_WITH_ME";
    public static final String SCREEN_SHARES_CREATED = "SHARES_CREATED";
    public static final String SCREEN_MY_KEYS = "MY_KEYS";
    public static final String SCREEN_ADMIN_AUDIT = "ADMIN_AUDIT";
    public static final String SCREEN_UPLOAD = "UPLOAD";
    public static final String SCREEN_RECEIVE = "RECEIVE";

    private JPanel container;
    private CardLayout cardLayout;
    private String currentScreen;

    private final Map<String, Component> registeredScreens = new LinkedHashMap<>();
    private final List<Consumer<String>> navigationListeners = new CopyOnWriteArrayList<>();

    public NavigationService() {}

    /**
     * Binds the CardLayout host container and layout manager.
     *
     * @param container  Parent JPanel holding screen cards
     * @param cardLayout CardLayout manager instance
     */
    public void bindContainer(JPanel container, CardLayout cardLayout) {
        this.container = container;
        this.cardLayout = cardLayout;
    }

    /**
     * Registers a named screen component.
     *
     * @param screenName Unique identifier
     * @param component  Screen component
     */
    public void registerScreen(String screenName, Component component) {
        if (screenName == null || screenName.trim().isEmpty()) {
            throw new IllegalArgumentException("Screen name cannot be null or empty");
        }
        if (component == null) {
            throw new IllegalArgumentException("Screen component cannot be null");
        }

        registeredScreens.put(screenName, component);
        if (container != null && cardLayout != null) {
            container.add(component, screenName);
        }
    }

    /**
     * Switches to the specified named screen.
     * WHAT: Changes CardLayout visible card and dispatches event to navigation listeners.
     * WHY: Ensures UI updates execute cleanly and observers (like sidebar selection) stay in sync.
     *
     * @param screenName Screen identifier
     */
    public void show(String screenName) {
        if (!registeredScreens.containsKey(screenName)) {
            throw new IllegalArgumentException("Unregistered screen: " + screenName);
        }

        this.currentScreen = screenName;

        if (container != null && cardLayout != null) {
            // Guarantee Swing GUI updates run on the Event Dispatch Thread
            if (SwingUtilities.isEventDispatchThread()) {
                cardLayout.show(container, screenName);
            } else {
                SwingUtilities.invokeLater(() -> cardLayout.show(container, screenName));
            }
        }

        // Notify navigation observers
        for (Consumer<String> listener : navigationListeners) {
            listener.accept(screenName);
        }
    }

    /**
     * Returns the currently active screen name.
     *
     * @return Screen name identifier
     */
    public String getCurrentScreen() {
        return currentScreen;
    }

    /**
     * Subscribes a listener to receive screen change notifications.
     *
     * @param listener Callback receiving target screen name
     */
    public void addListener(Consumer<String> listener) {
        if (listener != null) {
            navigationListeners.add(listener);
        }
    }

    /**
     * Removes a previously registered navigation listener.
     *
     * @param listener Callback to remove
     */
    public void removeListener(Consumer<String> listener) {
        navigationListeners.remove(listener);
    }

    /**
     * Checks if a screen name is registered.
     *
     * @param screenName Screen identifier
     * @return True if registered
     */
    public boolean hasScreen(String screenName) {
        return registeredScreens.containsKey(screenName);
    }
}
