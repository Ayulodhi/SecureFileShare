package com.securefileshare.client.ui;

import com.securefileshare.client.api.FileApi;
import com.securefileshare.client.model.ScanStatus;
import com.securefileshare.client.model.SharedFileDto;
import com.securefileshare.client.ui.components.*;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * "Shared with me" screen panel.
 * WHAT: Displays encrypted files shared with the current user by other platform participants.
 * WHY: Enables recipients to inspect incoming files, verify Clean scan status, and download/decrypt.
 */
public class SharedWithMePanel extends JPanel {

    private final FileApi fileApi;
    private final StateContainerPanel stateContainer;
    private final DataTable dataTable;
    private final DefaultTableModel tableModel;

    private List<SharedFileDto> currentFiles = List.of();

    public SharedWithMePanel(FileApi fileApi) {
        this.fileApi = fileApi;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.COLOR_APP_GROUND);
        setBorder(new EmptyBorder(24, 28, 20, 28));

        // 1. Header
        PageTitle pageTitle = new PageTitle("Shared with me", "Encrypted files shared with your public key by other users");
        add(pageTitle, BorderLayout.NORTH);

        // 2. Table setup
        String[] columns = {"Name", "Size", "Shared by", "Scan", "Expires", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };

        dataTable = new DataTable(tableModel, "No files have been shared with you yet.");
        setupTableRenderers();

        stateContainer = new StateContainerPanel(dataTable);
        add(stateContainer, BorderLayout.CENTER);

        loadSharedFiles();
    }

    private void setupTableRenderers() {
        JTable table = dataTable.getTable();

        // Scan Column (Index 3)
        table.getColumnModel().getColumn(3).setCellRenderer((tbl, value, isSelected, hasFocus, row, col) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p.setOpaque(true);
            p.setBackground(isSelected ? Theme.COLOR_LIGHT_TEAL : Theme.COLOR_CARD_WHITE);

            if (value instanceof ScanStatus status && status == ScanStatus.CLEAN) {
                p.add(new StatusChip("Clean", StatusChip.Variant.OK));
            } else {
                p.add(new StatusChip("Pending", StatusChip.Variant.WARNING));
            }
            return p;
        });

        // Action Column (Index 5): "Decrypt" SecondaryButton
        table.getColumnModel().getColumn(5).setCellRenderer(new TableCellRenderer() {
            private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            private final SecondaryButton decryptBtn = new SecondaryButton("Decrypt");

            {
                panel.setOpaque(true);
                decryptBtn.setPreferredSize(new Dimension(86, 34));
                panel.add(decryptBtn);
            }

            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                panel.setBackground(isSelected ? Theme.COLOR_LIGHT_TEAL : Theme.COLOR_CARD_WHITE);
                return panel;
            }
        });

        table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            private final SecondaryButton decryptBtn = new SecondaryButton("Decrypt");
            private int currentRow;

            {
                panel.setOpaque(true);
                decryptBtn.setPreferredSize(new Dimension(86, 34));
                decryptBtn.addActionListener(e -> {
                    fireEditingStopped();
                    if (currentRow >= 0 && currentRow < currentFiles.size()) {
                        SharedFileDto file = currentFiles.get(currentRow);
                        JOptionPane.showMessageDialog(SharedWithMePanel.this,
                                "Zero-knowledge decryption pipeline for '" + file.getFileName() + "' will activate in Phase C4.",
                                "Decrypt File", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
                panel.add(decryptBtn);
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

    public void loadSharedFiles() {
        stateContainer.showLoading("Loading files shared with you...");

        SwingWorker<List<SharedFileDto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<SharedFileDto> doInBackground() throws Exception {
                return fileApi.getSharedWithMe();
            }

            @Override
            protected void done() {
                try {
                    currentFiles = get();
                    tableModel.setRowCount(0);

                    for (SharedFileDto f : currentFiles) {
                        tableModel.addRow(new Object[]{
                                f.getFileName(),
                                f.getSize(),
                                f.getSharedBy(),
                                f.getScanStatus(),
                                f.getExpiresIn(),
                                true
                        });
                    }

                    dataTable.updateViewState();
                    stateContainer.showContent();
                } catch (Exception ex) {
                    stateContainer.showError("Failed to load shared files: " + ex.getMessage(), SharedWithMePanel.this::loadSharedFiles);
                }
            }
        };

        worker.execute();
    }
}
