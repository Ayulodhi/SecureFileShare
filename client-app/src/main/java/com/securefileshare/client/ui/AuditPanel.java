package com.securefileshare.client.ui;

import com.securefileshare.client.api.AuditApi;
import com.securefileshare.client.model.AuditAction;
import com.securefileshare.client.model.AuditEntryDto;
import com.securefileshare.client.model.AuditVerifyResult;
import com.securefileshare.client.ui.components.*;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Admin Audit Log screen panel.
 * WHAT: Displays paginated audit records (50 rows/page) with multi-criteria filters,
 *       a primary "Verify chain" cryptographic integrity trigger, dynamic green/red banners,
 *       and row error highlighting for broken blocks.
 * WHY: Strict compliance with Phase C6: Provides administrators with transparent, tamper-evident
 *      verification of all platform security operations.
 */
public class AuditPanel extends JPanel {

    private final AuditApi auditApi;
    private final StateContainerPanel stateContainer;
    private final DataTable dataTable;
    private final DefaultTableModel tableModel;

    // Filters
    private JTextField actorFilterField;
    private JComboBox<String> actionFilterCombo;
    private JComboBox<String> dateRangeCombo;

    // Banner
    private JPanel verifyBanner;
    private JLabel bannerIconLabel;
    private JLabel bannerTextLabel;

    // Controls & Pagination
    private PrimaryButton verifyChainButton;
    private JButton tamperToggleButton;
    private JLabel pageInfoLabel;
    private SecondaryButton prevButton;
    private SecondaryButton nextButton;

    // State
    private int currentPage = 1;
    private static final int PAGE_SIZE = 50; // Rule: 50 rows per page
    private int totalEntries = 0;
    private Long highlightedBrokenEntryId = null;
    private List<AuditEntryDto> currentEntries = List.of();

    public AuditPanel(AuditApi auditApi) {
        this.auditApi = auditApi;
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.COLOR_APP_GROUND);
        setBorder(new EmptyBorder(22, 28, 20, 28));

        // 1. Top Header with Title and "Verify chain" action
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. Central Section (Banner + Filter Bar + Table + Pagination)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Verification Outcome Banner (hidden by default)
        verifyBanner = createVerifyBanner();
        centerPanel.add(verifyBanner);
        centerPanel.add(Box.createVerticalStrut(10));

        // Filter Bar above table
        JPanel filterBar = createFilterBar();
        centerPanel.add(filterBar);
        centerPanel.add(Box.createVerticalStrut(12));

        // Table setup
        String[] columns = {"ID", "Time", "Actor", "Action", "File", "Result", "Hash"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        dataTable = new DataTable(tableModel, "No audit events matching criteria.");
        setupTableRenderers();

        // Wrap dataTable in pagination container
        JPanel tableWithPagination = new JPanel(new BorderLayout(0, 10));
        tableWithPagination.setOpaque(false);
        tableWithPagination.add(dataTable, BorderLayout.CENTER);

        JPanel paginationBar = createPaginationBar();
        tableWithPagination.add(paginationBar, BorderLayout.SOUTH);

        centerPanel.add(tableWithPagination);

        stateContainer = new StateContainerPanel(centerPanel);
        add(stateContainer, BorderLayout.CENTER);

        // Initial fetch
        loadAuditPage(1);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        PageTitle pageTitle = new PageTitle("Audit log", "Cryptographically linked tamper-evident SHA-256 hash chain");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        // Demo helper: Tamper test toggle button
        tamperToggleButton = new SecondaryButton("Tamper mode: OFF");
        tamperToggleButton.setFont(Theme.FONT_SMALL);
        tamperToggleButton.setPreferredSize(new Dimension(150, Theme.BUTTON_MIN_HEIGHT));
        tamperToggleButton.addActionListener(e -> {
            boolean current = auditApi.isTamperMode();
            auditApi.setTamperMode(!current);
            tamperToggleButton.setText("Tamper mode: " + (!current ? "ON" : "OFF"));
            tamperToggleButton.setForeground(!current ? Theme.CHIP_ERROR_TEXT : Theme.COLOR_PRIMARY_TEAL);
            // Refresh to apply tampered block
            loadAuditPage(currentPage);
        });

        // Rule: Primary "Verify chain" button
        verifyChainButton = new PrimaryButton("🛡️ Verify chain");
        verifyChainButton.setPreferredSize(new Dimension(140, Theme.BUTTON_MIN_HEIGHT));
        verifyChainButton.addActionListener(e -> executeChainVerification());

        actions.add(tamperToggleButton);
        actions.add(verifyChainButton);

        header.add(pageTitle, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel createVerifyBanner() {
        JPanel banner = new JPanel(new BorderLayout(10, 0));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        banner.setBorder(new EmptyBorder(10, 16, 10, 16));
        banner.setVisible(false);

        bannerIconLabel = new JLabel("✓");
        bannerIconLabel.setFont(Theme.FONT_BODY_BOLD);

        bannerTextLabel = new JLabel();
        bannerTextLabel.setFont(Theme.FONT_BODY_BOLD);

        banner.add(bannerIconLabel, BorderLayout.WEST);
        banner.add(bannerTextLabel, BorderLayout.CENTER);
        return banner;
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bar.setOpaque(false);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Actor Filter
        JLabel actorLabel = new JLabel("Actor:");
        actorLabel.setFont(Theme.FONT_LABEL);
        actorLabel.setForeground(Theme.COLOR_INK_NAVY);

        actorFilterField = Theme.createTextField(10);
        actorFilterField.setPreferredSize(new Dimension(130, 36));

        // Action Filter
        JLabel actionLabel = new JLabel("Action:");
        actionLabel.setFont(Theme.FONT_LABEL);
        actionLabel.setForeground(Theme.COLOR_INK_NAVY);

        actionFilterCombo = new JComboBox<>(new String[]{"All actions", "UPLOAD", "SHARE", "DOWNLOAD", "DENIED", "REVOKE"});
        actionFilterCombo.setFont(Theme.FONT_BODY);
        actionFilterCombo.setBackground(Color.WHITE);
        actionFilterCombo.setPreferredSize(new Dimension(130, 36));

        // Date Range Filter
        JLabel dateLabel = new JLabel("Date range:");
        dateLabel.setFont(Theme.FONT_LABEL);
        dateLabel.setForeground(Theme.COLOR_INK_NAVY);

        dateRangeCombo = new JComboBox<>(new String[]{"All time", "Last 24 hours", "Last 7 days", "Last 30 days"});
        dateRangeCombo.setFont(Theme.FONT_BODY);
        dateRangeCombo.setBackground(Color.WHITE);
        dateRangeCombo.setPreferredSize(new Dimension(130, 36));

        SecondaryButton filterButton = new SecondaryButton("Filter");
        filterButton.setPreferredSize(new Dimension(80, 36));
        filterButton.addActionListener(e -> {
            highlightedBrokenEntryId = null;
            verifyBanner.setVisible(false);
            loadAuditPage(1);
        });

        bar.add(actorLabel);
        bar.add(actorFilterField);
        bar.add(actionLabel);
        bar.add(actionFilterCombo);
        bar.add(dateLabel);
        bar.add(dateRangeCombo);
        bar.add(filterButton);

        return bar;
    }

    private JPanel createPaginationBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(4, 4, 0, 4));

        pageInfoLabel = new JLabel("Showing 0-0 of 0 entries | Page 1 of 1");
        pageInfoLabel.setFont(Theme.FONT_SMALL);
        pageInfoLabel.setForeground(Theme.COLOR_MUTED_TEXT);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        prevButton = new SecondaryButton("◀ Previous");
        prevButton.setPreferredSize(new Dimension(110, 34));
        prevButton.setEnabled(false);
        prevButton.addActionListener(e -> {
            if (currentPage > 1) {
                loadAuditPage(currentPage - 1);
            }
        });

        nextButton = new SecondaryButton("Next ▶");
        nextButton.setPreferredSize(new Dimension(95, 34));
        nextButton.setEnabled(false);
        nextButton.addActionListener(e -> {
            int maxPages = (int) Math.ceil((double) totalEntries / PAGE_SIZE);
            if (currentPage < maxPages) {
                loadAuditPage(currentPage + 1);
            }
        });

        buttons.add(prevButton);
        buttons.add(nextButton);

        bar.add(pageInfoLabel, BorderLayout.WEST);
        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    private void setupTableRenderers() {
        JTable table = dataTable.getTable();

        // Custom row renderer that highlights broken rows in red error color
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                c.setBorder(new EmptyBorder(0, 12, 0, 12));

                Long entryId = null;
                Object idVal = tbl.getValueAt(row, 0);
                if (idVal instanceof Long l) entryId = l;
                else if (idVal instanceof Integer i) entryId = i.longValue();
                else if (idVal != null) {
                    try { entryId = Long.parseLong(idVal.toString()); } catch (Exception ignored) {}
                }

                boolean isBroken = highlightedBrokenEntryId != null && highlightedBrokenEntryId.equals(entryId);

                if (isBroken) {
                    // Rule: Highlight that row in the error color
                    c.setBackground(Theme.CHIP_ERROR_BG);
                    c.setForeground(Theme.CHIP_ERROR_TEXT);
                    c.setFont(Theme.FONT_BODY_BOLD);
                } else if (isSelected) {
                    c.setBackground(Theme.COLOR_LIGHT_TEAL);
                    c.setForeground(Theme.COLOR_INK_NAVY);
                    c.setFont(Theme.FONT_BODY);
                } else {
                    c.setBackground(Theme.COLOR_CARD_WHITE);
                    c.setForeground(Theme.COLOR_INK_NAVY);
                    c.setFont(Theme.FONT_BODY);
                }

                // Monospace font for Hash column (Index 6)
                if (col == 6) {
                    c.setFont(Theme.FONT_MONO);
                    c.setForeground(isBroken ? Theme.CHIP_ERROR_TEXT : Theme.COLOR_DARK_TEAL_TEXT);
                }

                return c;
            }
        });

        // Action Column (Index 3): StatusChip renderer
        table.getColumnModel().getColumn(3).setCellRenderer((tbl, value, isSelected, hasFocus, row, col) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            p.setOpaque(true);

            Long entryId = null;
            Object idVal = tbl.getValueAt(row, 0);
            if (idVal != null) {
                try { entryId = Long.parseLong(idVal.toString()); } catch (Exception ignored) {}
            }
            boolean isBroken = highlightedBrokenEntryId != null && highlightedBrokenEntryId.equals(entryId);
            p.setBackground(isBroken ? Theme.CHIP_ERROR_BG : (isSelected ? Theme.COLOR_LIGHT_TEAL : Theme.COLOR_CARD_WHITE));

            if (value instanceof AuditAction action) {
                switch (action) {
                    case UPLOAD, SHARE, DOWNLOAD -> p.add(new StatusChip(action.name(), StatusChip.Variant.OK));
                    case REVOKE -> p.add(new StatusChip(action.name(), StatusChip.Variant.WARNING));
                    case DENIED -> p.add(new StatusChip(action.name(), StatusChip.Variant.ERROR));
                }
            } else if (value != null) {
                p.add(new JLabel(value.toString()));
            }
            return p;
        });
    }

    /**
     * Executes cryptographic hash chain verification and displays green/red banner.
     */
    private void executeChainVerification() {
        verifyChainButton.setEnabled(false);
        verifyBanner.setVisible(false);
        highlightedBrokenEntryId = null;
        dataTable.getTable().repaint();

        SwingWorker<AuditVerifyResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuditVerifyResult doInBackground() throws Exception {
                return auditApi.verifyChain();
            }

            @Override
            protected void done() {
                verifyChainButton.setEnabled(true);
                try {
                    AuditVerifyResult result = get();
                    showVerificationBanner(result);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AuditPanel.this, "Chain verification error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    /**
     * Displays the verification banner and highlights any broken row.
     * Rule: green "Chain valid, N entries checked", or red "Chain broken at entry <id>"
     *       and highlights that row in the error color.
     */
    public void showVerificationBanner(AuditVerifyResult result) {
        if (result.isValid()) {
            // Green valid banner
            verifyBanner.setBackground(Theme.CHIP_SUCCESS_BG);
            verifyBanner.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.CHIP_SUCCESS_TEXT, 1, true),
                    new EmptyBorder(10, 16, 10, 16)
            ));
            bannerIconLabel.setText("✓");
            bannerIconLabel.setForeground(Theme.CHIP_SUCCESS_TEXT);
            bannerTextLabel.setText(result.getMessage());
            bannerTextLabel.setForeground(Theme.CHIP_SUCCESS_TEXT);
            highlightedBrokenEntryId = null;
        } else {
            // Red broken banner
            verifyBanner.setBackground(Theme.CHIP_ERROR_BG);
            verifyBanner.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Theme.CHIP_ERROR_TEXT, 1, true),
                    new EmptyBorder(10, 16, 10, 16)
            ));
            bannerIconLabel.setText("✖");
            bannerIconLabel.setForeground(Theme.CHIP_ERROR_TEXT);
            bannerTextLabel.setText(result.getMessage() + " (Integrity check failed: Altered block payload or broken hash link)");
            bannerTextLabel.setForeground(Theme.CHIP_ERROR_TEXT);
            highlightedBrokenEntryId = result.getBrokenEntryId();
        }

        verifyBanner.setVisible(true);
        dataTable.getTable().repaint();
        revalidate();
        repaint();
    }

    /**
     * Loads paginated audit entries in a background worker.
     */
    public void loadAuditPage(int page) {
        stateContainer.showLoading("Loading audit records...");

        String actor = actorFilterField != null ? actorFilterField.getText().trim() : "";
        String actionStr = (String) (actionFilterCombo != null ? actionFilterCombo.getSelectedItem() : "All actions");
        AuditAction action = !"All actions".equalsIgnoreCase(actionStr) && actionStr != null ? AuditAction.valueOf(actionStr) : null;
        String dateRange = (String) (dateRangeCombo != null ? dateRangeCombo.getSelectedItem() : "All time");

        SwingWorker<List<AuditEntryDto>, Void> worker = new SwingWorker<>() {
            private int count;

            @Override
            protected List<AuditEntryDto> doInBackground() throws Exception {
                count = auditApi.getTotalCount(actor, action, dateRange);
                return auditApi.getAuditEntries(actor, action, dateRange, page, PAGE_SIZE);
            }

            @Override
            protected void done() {
                try {
                    currentEntries = get();
                    totalEntries = count;
                    currentPage = page;

                    tableModel.setRowCount(0);
                    for (AuditEntryDto e : currentEntries) {
                        tableModel.addRow(new Object[]{
                                e.getId(),
                                e.getTimestamp(),
                                e.getActor(),
                                e.getAction(),
                                e.getFile(),
                                e.getResult(),
                                e.getShortHash()
                        });
                    }

                    dataTable.updateViewState();
                    updatePaginationControls();
                    stateContainer.showContent();
                } catch (Exception ex) {
                    stateContainer.showError("Failed to load audit entries: " + ex.getMessage(), () -> loadAuditPage(page));
                }
            }
        };

        worker.execute();
    }

    private void updatePaginationControls() {
        int maxPages = Math.max(1, (int) Math.ceil((double) totalEntries / PAGE_SIZE));
        int start = totalEntries == 0 ? 0 : (currentPage - 1) * PAGE_SIZE + 1;
        int end = Math.min(currentPage * PAGE_SIZE, totalEntries);

        pageInfoLabel.setText("Showing " + start + "-" + end + " of " + totalEntries + " entries | Page " + currentPage + " of " + maxPages);
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < maxPages);
    }
}
