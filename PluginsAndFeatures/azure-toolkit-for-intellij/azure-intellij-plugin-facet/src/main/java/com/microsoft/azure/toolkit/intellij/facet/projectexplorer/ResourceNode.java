package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

public class ResourceNode extends AbstractTreeNode<Node<?>> implements IAzureFacetNode {
    public ResourceNode(@Nonnull Project project, final Node<?> node) {
        super(project, node);
        final NodeView view = node.view();
        view.setRefresher(new NodeView.Refresher() {
            @Override
            public void refreshView() {
                ResourceNode.this.update();
            }
        });
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final Node<?> node = this.getValue();
        return node.getChildren().stream().map(n -> new ResourceNode(this.getProject(), n)).toList();
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        final Node<?> node = this.getValue();
        final NodeView view = node.view();
        presentation.addText(view.getLabel(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        presentation.setIcon(IntelliJAzureIcons.getIcon(view.getIcon()));
        Optional.ofNullable(view.getDescription()).ifPresent(d -> presentation.addText(" " + d, SimpleTextAttributes.GRAYED_ATTRIBUTES));
    }

    @Override
    @Nullable
    public Object getData(@Nonnull String dataId) {
        if (StringUtils.equalsIgnoreCase(dataId, Action.SOURCE)) {
            return Optional.ofNullable(getValue()).map(Node::data).orElse(null);
        }
        return null;
    }

    @Override
    public void onDoubleClicked(Object event) {
        Optional.ofNullable(this.getValue()).ifPresent(n -> n.triggerDoubleClickAction(event));
    }

    @Override
    public void onClicked(Object event) {
        Optional.ofNullable(this.getValue()).ifPresent(n -> n.triggerClickAction(event));
    }

    @Override
    @Nullable
    public IActionGroup getActionGroup() {
        return Optional.ofNullable(getValue()).map(Node::actions).orElse(null);
    }
}