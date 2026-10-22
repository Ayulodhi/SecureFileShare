package com.securefileshare.client.ui.components;

import com.securefileshare.client.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * Reusable table component with card styling.
 * WHAT: Custom table container with white card surface, exactly 48px row height, #F4F6F8 header background,
 *       and an integrated empty-state message when no rows exist.
 * WHY: Strict compliance with Phase C2 design specifications: eliminates duplicate table setup
 *      and guarantees uniform zero-knowledge data presentation across all screens.
 */
public class DataTable extends Card {

    private final JTable table;
    private final JScrollPane scrollPane;
    private final JPanel emptyStatePanel;
    private final JLabel emptyStateLabel;
    private final CardLayout displayLayout;
    private final JPanel displayCards;

    private static final String VIEW_TABLE = "TABLE";
    private static final String VIEW_EMPTY = "EMPTY";

    public DataTable(TableModel model, String emptyMessage) {
        super(new BorderLayout());

        table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Table cells are read-only; button columns handle clicks through editors/renderers
                return getCellEditor(row, column) != null;
            }
        };

        // Rule: Exactly 48px row height
        table.setRowHeight(48);
        table.setFont(Theme.FONT_BODY);
        table.setForeground(Theme.COLOR_INK_NAVY);
        table.setBackground(Theme.COLOR_CARD_WHITE);
        table.setSelectionBackground(Theme.COLOR_LIGHT_TEAL);
        table.setSelectionForeground(Theme.COLOR_INK_NAVY);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xEE, 0xF2, 0xF5));
        table.setFillsViewportHeight(true);

        // Rule: Header row in #F4F6F8
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.FONT_LABEL);
        header.setForeground(Theme.COLOR_INK_NAVY);
        header.setBackground(Theme.COLOR_APP_GROUND);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setBorder(new MatteBorder(0, 0, 1, 0, Theme.COLOR_BORDER));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                l.setBackground(Theme.COLOR_APP_GROUND);
                l.setForeground(Theme.COLOR_INK_NAVY);
                l.setFont(Theme.FONT_LABEL);
                l.setBorder(new EmptyBorder(0, 14, 0, 14));
                return l;
            }
        });

        // Default cell renderer with clean left margin
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
        defaultRenderer.setBorder(new EmptyBorder(0, 14, 0, 14));
        table.setDefaultRenderer(Object.class, defaultRenderer);

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Theme.COLOR_CARD_WHITE);

        // Empty state view
        emptyStatePanel = new JPanel();
        emptyStatePanel.setLayout(new BoxLayout(emptyStatePanel, BoxLayout.Y_AXIS));
        emptyStatePanel.setOpaque(false);
        emptyStatePanel.setBorder(new EmptyBorder(40, 20, 40, 20));

        JLabel emptyIcon = new JLabel("📭");
        emptyIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyStateLabel = new JLabel(emptyMessage != null ? emptyMessage : "No items to display.");
        emptyStateLabel.setFont(Theme.FONT_BODY);
        emptyStateLabel.setForeground(Theme.COLOR_MUTED_TEXT);
        emptyStateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyStatePanel.add(emptyIcon);
        emptyStatePanel.add(Box.createVerticalStrut(10));
        emptyStatePanel.add(emptyStateLabel);

        // CardLayout container to toggle between table and empty state
        displayLayout = new CardLayout();
        displayCards = new JPanel(displayLayout);
        displayCards.setOpaque(false);
        displayCards.add(scrollPane, VIEW_TABLE);
        displayCards.add(emptyStatePanel, VIEW_EMPTY);

        add(displayCards, BorderLayout.CENTER);

        updateViewState();
    }

    /**
     * Toggles between Table view and Empty State view based on row count.
     */
    public void updateViewState() {
        if (table.getModel().getRowCount() == 0) {
            displayLayout.show(displayCards, VIEW_EMPTY);
        } else {
            displayLayout.show(displayCards, VIEW_TABLE);
        }
    }

    public JTable getTable() {
        return table;
    }

    public void setEmptyMessage(String message) {
        emptyStateLabel.setText(message);
    }
}
