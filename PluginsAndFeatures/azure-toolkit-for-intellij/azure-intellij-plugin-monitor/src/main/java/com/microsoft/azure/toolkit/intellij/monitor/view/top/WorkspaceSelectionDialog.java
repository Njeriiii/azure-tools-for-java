package com.microsoft.azure.toolkit.intellij.monitor.view.top;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class WorkspaceSelectionDialog extends DialogWrapper {
    private JPanel centerPanel;
    private JComboBox subComboBox;
    private JComboBox rgComboBox;
    private JComboBox workspaceComboBox;

    public WorkspaceSelectionDialog(@Nullable final Project project) {
        super(project, false);
    }


    @Override
    protected @Nullable JComponent createCenterPanel() {
        return centerPanel;
    }

    private void createUIComponents() {
        subComboBox = new SubscriptionComboBox();
        rgComboBox = new ResourceGroupComboBox();
//        workspaceComboBox =
    }

}
