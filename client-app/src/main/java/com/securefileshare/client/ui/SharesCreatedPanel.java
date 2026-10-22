package com.securefileshare.client.ui;

import com.securefileshare.client.api.ShareApi;
import com.securefileshare.client.model.ShareRecordDto;
import com.securefileshare.client.model.ShareStatus;
import com.securefileshare.client.ui.components.*;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * "Shares I created" screen panel.
 * WHAT: Displays issued file share grants with lifecycle status (Active / Expired / Revoked)
 *       and an active "Revoke" button allowing instantaneous permission revocation.
 * WHY: Strict compliance with Phase C2: Users must be able to audit and revoke recipient access
 *      grants without leaving the client desktop.
 */
public class SharesCreatedPanel extends JPanel {

    private final ShareApi shareApi;
    private final StateContainerPanel stateContainer;
    private final DataTable dataTable;
    private final DefaultTableModel tableModel;

    private List<ShareRecordDto> currentShares = List.of();

    public SharesCreatedPanel(ShareApi shareApi) {
        this.shareApi = shareApi;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.COLOR_APP_GROUND);
        setBorder(new EmptyBorder(24, 28, 20, 28));

        // 1. Header
        PageTitle pageTitle = new PageTitle("Shares I created", "Manage and instantaneously revoke recipient file access grants");
        add(pageTitle, BorderLayout.NORTH);

        // 2. Table setup
        String[] columns = {"File name", "Shared with", "Status", "Created on", "Expires on", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };

        dataTable = new DataTable(tableModel, "You have not shared any files yet.");
        setupTableRenderers();

        stateContainer = new StateContainerPanel(dataTable);
        add(stateContainer, BorderLayout.CENTER);

        loadShares();
    }

    private void setupTableRenderers() {
        JTable table = dataTable.getTable();

        // Status Column (Index 2): StatusChip Active / Expired / Revoked
        table.getColumnModel().getColumn(2).setCellRenderer((tbl, value, isSelected, hasFocus, row, col) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p.setOpaque(true);
            p.setBackground(isSelected ? Theme.COLOR_LIGHT_TEAL : Theme.COLOR_CARD_WHITE);

            if (value instanceof ShareStatus status) {
                switch (status) {
                    case ACTIVE -> p.add(new StatusChip("Active", StatusChip.Variant.OK));
                    case EXPIRED -> p.add(new StatusChip("Expired", StatusChip.Variant.WARNING));
                    case REVOKED -> p.add(new StatusChip("Revoked", StatusChip.Variant.ERROR));
                }
            }
            return p;
        });

        // Action Column (Index 5): "Revoke" SecondaryButton or "-"
        table.getColumnModel().getColumn(5).setCellRenderer(new TableCellRenderer() {
            private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            private final SecondaryButton revokeBtn = new SecondaryButton("Revoke");
            private final JLabel inactiveLabel = new JLabel("—");

            {
                panel.setOpaque(true);
                revokeBtn.setPreferredSize(new Dimension(84, 34));
                inactiveLabel.setFont(Theme.FONT_BODY);
                inactiveLabel.setForeground(Theme.COLOR_MUTED_TEXT);
            }

            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                panel.setBackground(isSelected ? Theme.COLOR_LIGHT_TEAL : Theme.COLOR_CARD_WHITE);
                panel.removeAll();

                boolean canRevoke = Boolean.TRUE.equals(value);
                if (canRevoke) {
                    panel.add(revokeBtn);
                } else {
                    panel.add(inactiveLabel);
                }
                return panel;
            }
        });

        // Action Editor for handling Revoke clicks
        table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            private final SecondaryButton revokeBtn = new SecondaryButton("Revoke");
            private int currentRow;

            {
                panel.setOpaque(true);
                revokeBtn.setPreferredSize(new Dimension(84, 34));
                revokeBtn.addActionListener(e -> {
                    fireEditingStopped();
                    if (currentRow >= 0 && currentRow < currentShares.size()) {
                        ShareRecordDto record = currentShares.get(currentRow);
                        performRevoke(record, currentRow);
                    }
                });
                panel.add(revokeBtn);
            }

            @Override
            public Component getTableCellEditorComponent(JTable tbl, Object value, boolean isSelected, int row, int col) {
                this.currentRow = row;
                panel.setBackground(Theme.COLOR_LIGHT_TEAL);
                return panel;
            }

            @Override
            public Object getCellEditorValue() {
                return true;
            }
        });
    }

    /**
     * Executes revocation via ShareApi on a background thread.
     */
    private void performRevoke(ShareRecordDto record, int row) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Revoke recipient '" + record.getRecipient() + "' access to '" + record.getFileName() + "' immediately?\n" +
                        "Why: Revocation stops any further key unwrapping on the recipient client.",
                "Confirm Access Revocation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                shareApi.revokeShare(record.getShareId());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    // Update in-memory record and table row
                    record.setStatus(ShareStatus.REVOKED);
                    tableModel.setValueAt(ShareStatus.REVOKED, row, 2);
                    tableModel.setValueAt(false, row, 5);

                    JOptionPane.showMessageDialog(
                            SharesCreatedPanel.this,
                            "Access for '" + record.getRecipient() + "' has been revoked successfully.",
                            "Access Revoked",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            SharesCreatedPanel.this,
                            "Failed to revoke share: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    public void loadShares() {
        stateContainer.showLoading("Loading share permissions...");

        SwingWorker<List<ShareRecordDto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ShareRecordDto> doInBackground() throws Exception {
                return shareApi.getSharesCreated();
            }

            @Override
            protected void done() {
                try {
                    currentShares = get();
                    tableModel.setRowCount(0);

                    for (ShareRecordDto s : currentShares) {
                        tableModel.addRow(new Object[]{
                                s.getFileName(),
                                s.getRecipient(),
                                s.getStatus(),
                                s.getCreatedAt(),
                                s.getExpiresAt(),
                                s.canRevoke()
                        });
                    }

                    dataTable.updateViewState();
                    stateContainer.showContent();
                } catch (Exception ex) {
                    stateContainer.showError("Failed to load shares: " + ex.getMessage(), SharesCreatedPanel.this::loadShares);
                }
            }
        };

        worker.execute();
    }
}
