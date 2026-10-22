package com.securefileshare.client.ui;

import com.securefileshare.client.api.FileApi;
import com.securefileshare.client.controller.ShareController;
import com.securefileshare.client.model.FileDto;
import com.securefileshare.client.model.ScanStatus;
import com.securefileshare.client.ui.components.*;
import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * "My files" screen panel.
 * WHAT: Displays owned zero-knowledge encrypted files in a 48px row table with scan status chips,
 *       sharing recipients, per-row Share actions, an "Upload file" header button, and footer metadata notice.
 * WHY: Primary user files screen. Demonstrates zero-knowledge visible guarantees: Quarantined files
 *      show amber chips and "Not shareable", preventing distribution of unverified payloads.
 */
public class MyFilesPanel extends JPanel {

    private final FileApi fileApi;
    private final StateContainerPanel stateContainer;
    private final DataTable dataTable;
    private final DefaultTableModel tableModel;

    private List<FileDto> currentFiles = List.of();
    private BiConsumer<File, String> uploadInitiationListener;
    private ShareController shareController;
    private Runnable onShareCreatedListener;

    public MyFilesPanel(FileApi fileApi) {
        this.fileApi = fileApi;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.COLOR_APP_GROUND);
        setBorder(new EmptyBorder(24, 28, 20, 28));

        // 1. Top Header: Title on left, "Upload file" primary button on right
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        PageTitle pageTitle = new PageTitle("My files", "Zero-knowledge end-to-end encrypted files stored in your vault");
        PrimaryButton uploadButton = new PrimaryButton("+ Upload file");
        uploadButton.addActionListener(e -> triggerUploadDialog());

        headerPanel.add(pageTitle, BorderLayout.WEST);
        headerPanel.add(uploadButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Center Content: Table within Card styling
        String[] columnNames = {"Name", "Size", "Scan", "Shared with", "Action"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Only the Action column is interactive
                return column == 4;
            }
        };

        dataTable = new DataTable(tableModel, "No files uploaded yet. Click '+ Upload file' to encrypt your first document.");
        setupTableRenderers();

        // 3. Bottom Footer Note
        JLabel footerNote = new JLabel("Everything listed here is stored encrypted. Names and sizes are the only readable metadata.");
        footerNote.setFont(Theme.FONT_SMALL);
        footerNote.setForeground(Theme.COLOR_MUTED_TEXT);
        footerNote.setBorder(new EmptyBorder(8, 4, 0, 0));

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 8));
        centerWrapper.setOpaque(false);
        centerWrapper.add(dataTable, BorderLayout.CENTER);
        centerWrapper.add(footerNote, BorderLayout.SOUTH);

        // State container wrapper managing loading spinner and inline errors
        stateContainer = new StateContainerPanel(centerWrapper);
        add(stateContainer, BorderLayout.CENTER);

        // Initial background data fetch
        loadFiles();
    }

    public void setShareController(ShareController shareController) {
        this.shareController = shareController;
    }

    public void setOnShareCreatedListener(Runnable onShareCreatedListener) {
        this.onShareCreatedListener = onShareCreatedListener;
    }

    public void setUploadInitiationListener(BiConsumer<File, String> uploadInitiationListener) {
        this.uploadInitiationListener = uploadInitiationListener;
    }

    private void triggerUploadDialog() {
        Window ancestor = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = ancestor instanceof Frame ? (Frame) ancestor : null;

        UploadDialog dialog = new UploadDialog(parentFrame, (file, recipient) -> {
            if (uploadInitiationListener != null) {
                uploadInitiationListener.accept(file, recipient);
            }
        });
        dialog.setVisible(true);
    }

    private void openShareDialog(FileDto file) {
        Window ancestor = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = ancestor instanceof Frame ? (Frame) ancestor : null;

        if (shareController != null) {
            ShareDialog shareDialog = new ShareDialog(parentFrame, shareController, file, () -> {
                if (onShareCreatedListener != null) {
                    onShareCreatedListener.run();
                }
            });
            shareDialog.setVisible(true);
        }
    }

    /**
     * Configures cell renderers for Scan StatusChip and Action buttons.
     */
    private void setupTableRenderers() {
        JTable table = dataTable.getTable();

        // Scan Column (Index 2): StatusChip renderer
        table.getColumnModel().getColumn(2).setCellRenderer((tbl, value, isSelected, hasFocus, row, col) -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            p.setOpaque(true);
            p.setBackground(isSelected ? Theme.COLOR_LIGHT_TEAL : Theme.COLOR_CARD_WHITE);

            if (value instanceof ScanStatus status) {
                if (status == ScanStatus.CLEAN) {
                    p.add(new StatusChip("Clean", StatusChip.Variant.OK));
                } else if (status == ScanStatus.QUARANTINED) {
                    p.add(new StatusChip("Quarantined", StatusChip.Variant.WARNING));
                } else {
                    p.add(new StatusChip("Pending", StatusChip.Variant.WARNING));
                }
            }
            return p;
        });

        // Action Column (Index 4): SecondaryButton "Share" or "Not shareable"
        table.getColumnModel().getColumn(4).setCellRenderer(new TableCellRenderer() {
            private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            private final SecondaryButton shareBtn = new SecondaryButton("Share");
            private final JLabel notShareableLabel = new JLabel("Not shareable");

            {
                panel.setOpaque(true);
                shareBtn.setPreferredSize(new Dimension(80, 34));
                notShareableLabel.setFont(Theme.FONT_SMALL);
                notShareableLabel.setForeground(Theme.COLOR_MUTED_TEXT);
            }

            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                panel.setBackground(isSelected ? Theme.COLOR_LIGHT_TEAL : Theme.COLOR_CARD_WHITE);
                panel.removeAll();

                boolean canShare = Boolean.TRUE.equals(value);
                if (canShare) {
                    panel.add(shareBtn);
                } else {
                    panel.add(notShareableLabel);
                }
                return panel;
            }
        });

        // Action Column Editor for handling Share button clicks
        table.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            private final SecondaryButton shareBtn = new SecondaryButton("Share");
            private int currentRow;

            {
                panel.setOpaque(true);
                shareBtn.setPreferredSize(new Dimension(80, 34));
                shareBtn.addActionListener(e -> {
                    fireEditingStopped();
                    if (currentRow >= 0 && currentRow < currentFiles.size()) {
                        FileDto file = currentFiles.get(currentRow);
                        // Open 760x520 modal ShareDialog
                        openShareDialog(file);
                    }
                });
                panel.add(shareBtn);
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
     * Loads files from FileApi in a background SwingWorker to avoid freezing the EDT.
     */
    public void loadFiles() {
        stateContainer.showLoading("Loading your encrypted files...");

        SwingWorker<List<FileDto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<FileDto> doInBackground() throws Exception {
                return fileApi.getMyFiles();
            }

            @Override
            protected void done() {
                try {
                    currentFiles = get();
                    tableModel.setRowCount(0);

                    for (FileDto f : currentFiles) {
                        tableModel.addRow(new Object[]{
                                f.getName(),
                                f.getFormattedSize(),
                                f.getScanStatus(),
                                f.getSharedWith(),
                                f.isCanShare()
                        });
                    }

                    dataTable.updateViewState();
                    stateContainer.showContent();
                } catch (Exception ex) {
                    stateContainer.showError("Failed to load files: " + ex.getMessage(), MyFilesPanel.this::loadFiles);
                }
            }
        };

        worker.execute();
    }
}
