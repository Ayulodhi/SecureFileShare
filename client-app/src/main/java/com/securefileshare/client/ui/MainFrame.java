package com.securefileshare.client.ui;

import com.securefileshare.client.api.AuditApi;
import com.securefileshare.client.api.FileApi;
import com.securefileshare.client.api.KeyApi;
import com.securefileshare.client.api.ShareApi;
import com.securefileshare.client.api.mock.*;
import com.securefileshare.client.api.real.RealAuditApi;
import com.securefileshare.client.config.AppConfig;
import com.securefileshare.client.controller.NavigationService;
import com.securefileshare.client.controller.ReceiveController;
import com.securefileshare.client.controller.ShareController;
import com.securefileshare.client.controller.UploadController;
import com.securefileshare.client.service.KnownFingerprintsService;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Main application window frame for Phase C6.
 * WHAT: Houses the 210px SidebarPanel and CardLayout container holding MyFilesPanel,
 *       SharedWithMePanel, SharesCreatedPanel, MyKeysPanel, UploadProgressPanel,
 *       ReceivePanel, and the Admin AuditPanel.
 * WHY: Wires the tamper-evident audit screen and manages role-based access control (RBAC)
 *      to hide the audit log entry from non-admin users.
 */
public class MainFrame extends JFrame {

    private final NavigationService navigationService;
    private final SidebarPanel sidebarPanel;
    private final CardLayout cardLayout;
    private final JPanel cardsContainer;

    private final MyFilesPanel myFilesPanel;
    private final SharedWithMePanel sharedWithMePanel;
    private final SharesCreatedPanel sharesCreatedPanel;
    private final MyKeysPanel myKeysPanel;
    private final UploadProgressPanel uploadProgressPanel;
    private final ReceivePanel receivePanel;
    private final AuditPanel auditPanel;

    private final UploadController uploadController;
    private final ShareController shareController;
    private final ReceiveController receiveController;
    private final AuditApi auditApi;

    public MainFrame(NavigationService navigationService) {
        this(
                navigationService,
                new MockFileApi(),
                new MockShareApi(),
                new MockKeyApi(),
                new UploadController(new StubMalwareScanner(), new StubEncryptionService(), new StubUploadApi()),
                new ShareController(new MockShareApi(), new KnownFingerprintsService()),
                new ReceiveController(new MockShareApi(), new StubEncryptionService(), new StubMalwareScanner()),
                AppConfig.getInstance().isMockMode() ? new MockAuditApi() : new RealAuditApi()
        );
    }

    public MainFrame(NavigationService navigationService, FileApi fileApi, ShareApi shareApi, KeyApi keyApi,
                     UploadController uploadController, ShareController shareController,
                     ReceiveController receiveController, AuditApi auditApi) {
        // Window title bar area is the normal OS title bar (do not draw a fake one)
        super("SecureFileShare - Zero-Knowledge Secure File Platform");
        this.navigationService = navigationService;
        this.uploadController = uploadController;
        this.shareController = shareController;
        this.receiveController = receiveController;
        this.auditApi = auditApi;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Rule: 1100x640, centered, minimum 900x560
        setSize(1100, 640);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null); // Center on screen

        getContentPane().setBackground(Theme.COLOR_APP_GROUND);
        setLayout(new BorderLayout());

        // Left 210px Sidebar
        sidebarPanel = new SidebarPanel(navigationService);
        sidebarPanel.setUsername("rahul.sharma");
        // Rule: Hide the sidebar entry for non-admin users (default to admin=true for testing or false for standard user)
        sidebarPanel.setAdmin(true);
        add(sidebarPanel, BorderLayout.WEST);

        // Center CardLayout Screen Container
        cardLayout = new CardLayout();
        cardsContainer = new JPanel(cardLayout);
        cardsContainer.setBackground(Theme.COLOR_APP_GROUND);

        navigationService.bindContainer(cardsContainer, cardLayout);

        // Instantiate Dashboard Panels
        myFilesPanel = new MyFilesPanel(fileApi);
        sharedWithMePanel = new SharedWithMePanel(fileApi);
        sharesCreatedPanel = new SharesCreatedPanel(shareApi);
        myKeysPanel = new MyKeysPanel(keyApi);

        // Wire ShareController into MyFilesPanel and refresh SharesCreatedPanel on success
        myFilesPanel.setShareController(shareController);
        myFilesPanel.setOnShareCreatedListener(sharesCreatedPanel::loadShares);

        // Instantiate Phase C3 Upload Progress Panel
        uploadProgressPanel = new UploadProgressPanel(
                uploadController,
                navigationService,
                () -> {
                    navigationService.show(NavigationService.SCREEN_MY_FILES);
                    myFilesPanel.loadFiles();
                }
        );

        // Wire "+ Upload file" in MyFilesPanel to switch to Upload screen
        myFilesPanel.setUploadInitiationListener((file, recipient) -> {
            navigationService.show(NavigationService.SCREEN_UPLOAD);
            uploadProgressPanel.startUpload(file, recipient);
        });

        // Instantiate Phase C5 Receive Panel ("Receive a file")
        receivePanel = new ReceivePanel(receiveController, navigationService);

        // Instantiate Phase C6 Admin Audit Panel ("Audit log")
        auditPanel = new AuditPanel(auditApi);

        // Register panels into navigation service
        navigationService.registerScreen(NavigationService.SCREEN_MY_FILES, myFilesPanel);
        navigationService.registerScreen(NavigationService.SCREEN_SHARED_WITH_ME, sharedWithMePanel);
        navigationService.registerScreen(NavigationService.SCREEN_RECEIVE, receivePanel);
        navigationService.registerScreen(NavigationService.SCREEN_SHARES_CREATED, sharesCreatedPanel);
        navigationService.registerScreen(NavigationService.SCREEN_MY_KEYS, myKeysPanel);
        navigationService.registerScreen(NavigationService.SCREEN_UPLOAD, uploadProgressPanel);
        navigationService.registerScreen(NavigationService.SCREEN_ADMIN_AUDIT, auditPanel);

        add(cardsContainer, BorderLayout.CENTER);

        // Initial default navigation target
        navigationService.show(NavigationService.SCREEN_MY_FILES);
    }

    /**
     * Toggles role-based visibility of the Admin audit sidebar item.
     * Rule: Hide the sidebar entry for non-admin users.
     *
     * @param isAdmin True for ROLE_ADMIN, false for normal users
     */
    public void setAdminRole(boolean isAdmin) {
        sidebarPanel.setAdmin(isAdmin);
    }

    public SidebarPanel getSidebarPanel() {
        return sidebarPanel;
    }

    public MyFilesPanel getMyFilesPanel() {
        return myFilesPanel;
    }

    public ReceivePanel getReceivePanel() {
        return receivePanel;
    }

    public UploadProgressPanel getUploadProgressPanel() {
        return uploadProgressPanel;
    }

    public SharesCreatedPanel getSharesCreatedPanel() {
        return sharesCreatedPanel;
    }

    public AuditPanel getAuditPanel() {
        return auditPanel;
    }

    public AuditApi getAuditApi() {
        return auditApi;
    }
}
