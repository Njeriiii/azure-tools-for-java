package com.microsoft.azure.toolkit.intellij.monitor.view.right;

import com.intellij.ui.JBSplitter;
import com.microsoft.azure.toolkit.intellij.monitor.view.AzureMonitorView;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class MonitorLogPanel {
    @Getter
    private final JBSplitter splitter;
    private final MonitorLogTablePanel monitorLogTablePanel;
    private final MonitorLogDetailsPanel monitorLogDetailsPanel;
    @Setter
    private boolean isTableTab;
    @Setter
    private AzureMonitorView parentView;

    public MonitorLogPanel() {
        this.splitter = new JBSplitter();
        this.monitorLogTablePanel = new MonitorLogTablePanel();
        this.monitorLogDetailsPanel = new MonitorLogDetailsPanel();
        this.splitter.setProportion(0.8f);
        this.splitter.setFirstComponent(this.monitorLogTablePanel.getContentPanel());
        this.splitter.setSecondComponent(this.monitorLogDetailsPanel.getContentPanel());
        this.initListener();
    }

    public void refresh() {
        this.monitorLogTablePanel.setTableTab(this.isTableTab);
        executeQuery();
    }

    private void initListener() {
        this.monitorLogTablePanel.addTableSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                return;
            }
            this.monitorLogDetailsPanel.setStatus("Loading");
            this.monitorLogDetailsPanel.setViewText(monitorLogTablePanel.getSelectedColumnName(), monitorLogTablePanel.getSelectedCellValue());
        });
        this.monitorLogTablePanel.addRunActionListener(e -> executeQuery());
    }

    private void executeQuery() {
        final LogAnalyticsWorkspace selectedWorkspace = this.parentView.getSelectedWorkspace();
        if (Objects.isNull(selectedWorkspace)) {
            AzureMessager.getMessager().warning("Please select log analytics workspace first");
            return;
        }
        final String queryString = this.isTableTab ? this.monitorLogTablePanel.getQueryStringFromFilters(this.parentView.getCurrentTreeNodeText()) : this.parentView.getCurrentTreeNodeText();
        this.monitorLogTablePanel.loadTableModel(selectedWorkspace, queryString);
        this.monitorLogDetailsPanel.setStatus("No table cell is selected");
    }
}
