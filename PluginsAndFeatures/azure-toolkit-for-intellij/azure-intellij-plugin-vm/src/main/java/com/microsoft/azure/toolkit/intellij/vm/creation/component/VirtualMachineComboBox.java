/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupCreationDialog;
import com.microsoft.azure.toolkit.intellij.vm.creation.VMCreationDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.compute.AzureCompute;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImage;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VirtualMachineComboBox extends AzureComboBox<VirtualMachine> {
    private final List<VirtualMachine> draftItems = new ArrayList<>();

    @Override
    protected String getItemText(Object item) {
        return item instanceof VirtualMachine ? ((VirtualMachine) item).getName() : super.getItemText(item);
    }

//    @Nullable
//    @Override
//    protected ExtendableTextComponent.Extension getExtension() {
//        return ExtendableTextComponent.Extension.create(
//            AllIcons.General.Add, AzureMessageBundle.message("vm.create.tooltip").toString(), this::showVirtualMachineCreationPopup);
//    }

    @Nonnull
    @Override
    protected List<? extends VirtualMachine> loadItems() {
        final List<VirtualMachine> groups = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(this.draftItems)) {
            groups.addAll(new ArrayList<>(this.draftItems));
        }
        final List<VirtualMachine> remoteGroups = Azure.az(AzureCompute.class).virtualMachines().stream()
            .sorted(Comparator.comparing(VirtualMachine::getName)).collect(Collectors.toList());
        groups.addAll(remoteGroups);
        return groups;
    }

    private void showVirtualMachineCreationPopup() {
        final VMCreationDialog dialog = new VMCreationDialog(null);
        dialog.setOkActionListener((vm) -> {
            this.draftItems.add(0, vm);
            dialog.close();
            final List<VirtualMachine> items = new ArrayList<>(this.getItems());
            items.add(0, vm);
            this.setItems(items);
            this.setValue(vm);
        });
        dialog.show();
    }
}
