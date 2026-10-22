package com.securefileshare.client.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the NavigationService.
 * WHAT: Verifies screen registration, card transitions, active screen tracking,
 *       and listener notification.
 * WHY: Ensures decoupled screen navigation operates reliably without UI regressions.
 */
class NavigationServiceTest {

    private NavigationService navigationService;
    private JPanel container;
    private CardLayout cardLayout;

    @BeforeEach
    void setUp() {
        navigationService = new NavigationService();
        cardLayout = new CardLayout();
        container = new JPanel(cardLayout);
        navigationService.bindContainer(container, cardLayout);
    }

    @Test
    @DisplayName("registerScreen should store screen and allow hasScreen check")
    void testRegisterScreen() {
        JPanel testPanel = new JPanel();
        navigationService.registerScreen(NavigationService.SCREEN_MY_FILES, testPanel);

        assertThat(navigationService.hasScreen(NavigationService.SCREEN_MY_FILES)).isTrue();
        assertThat(navigationService.hasScreen("NON_EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("registerScreen should throw IllegalArgumentException on null screen name or component")
    void testRegisterScreenValidation() {
        assertThatThrownBy(() -> navigationService.registerScreen(null, new JPanel()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> navigationService.registerScreen("", new JPanel()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> navigationService.registerScreen("VALID_NAME", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("show should update current screen and notify registered listeners")
    void testShowAndListenerNotification() {
        navigationService.registerScreen(NavigationService.SCREEN_MY_FILES, new JPanel());
        navigationService.registerScreen(NavigationService.SCREEN_MY_KEYS, new JPanel());

        AtomicReference<String> navigatedScreen = new AtomicReference<>();
        navigationService.addListener(navigatedScreen::set);

        navigationService.show(NavigationService.SCREEN_MY_FILES);
        assertThat(navigationService.getCurrentScreen()).isEqualTo(NavigationService.SCREEN_MY_FILES);
        assertThat(navigatedScreen.get()).isEqualTo(NavigationService.SCREEN_MY_FILES);

        navigationService.show(NavigationService.SCREEN_MY_KEYS);
        assertThat(navigationService.getCurrentScreen()).isEqualTo(NavigationService.SCREEN_MY_KEYS);
        assertThat(navigatedScreen.get()).isEqualTo(NavigationService.SCREEN_MY_KEYS);
    }

    @Test
    @DisplayName("show should throw IllegalArgumentException for an unregistered screen")
    void testShowUnregisteredScreen() {
        assertThatThrownBy(() -> navigationService.show("UNKNOWN_SCREEN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unregistered screen");
    }

    @Test
    @DisplayName("removeListener should prevent subsequent notifications")
    void testRemoveListener() {
        navigationService.registerScreen(NavigationService.SCREEN_MY_FILES, new JPanel());

        AtomicReference<String> navigatedScreen = new AtomicReference<>();
        java.util.function.Consumer<String> listener = navigatedScreen::set;

        navigationService.addListener(listener);
        navigationService.removeListener(listener);

        navigationService.show(NavigationService.SCREEN_MY_FILES);
        assertThat(navigatedScreen.get()).isNull();
    }
}
