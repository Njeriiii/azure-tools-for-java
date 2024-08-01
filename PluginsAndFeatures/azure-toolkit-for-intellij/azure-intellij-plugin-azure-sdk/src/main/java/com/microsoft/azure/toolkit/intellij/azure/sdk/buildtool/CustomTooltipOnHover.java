package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBPanel;
import com.intellij.openapi.editor.EditorFactory;
import org.jetbrains.annotations.NotNull;

import java.awt.Insets;

import static com.intellij.ui.ColorUtil.toHex;

/**
 * This class is used to create a tooltip with a recommendation text and a link to the Azure SDK for Java documentation.
 */
class CustomTooltipOnHover implements LocalQuickFix {

    private final String recommendationText;
    private final String linkUrl;

    /**
     * Constructor for CustomTooltipOnHover.
     *
     * @param recommendationText - the recommendation text to be shown in the tooltip
     * @param linkUrl            - the URL to be opened when the user clicks on the link in the tooltip
     */
    CustomTooltipOnHover(String recommendationText, String linkUrl) {
        this.recommendationText = recommendationText;
        this.linkUrl = linkUrl;
    }

    /**
     * This method is used to get the name of the quick fix.
     *
     * @return The name of the quick fix.
     */
    @Override
    public @NotNull String getName() {
        return "Show Details";
    }

    /**
     * This method is used to get the family name of the quick fix.
     * The family name is used to group similar quick fixes together in the UI.
     *
     * @return The family name of the quick fix.
     */
    @Override
    public @NotNull String getFamilyName() {
        return getName();
    }

    /**
     * This method is used to display the tooltip when the user clicks on the "Show Details" link in the tooltip.
     *
     * @param project    The project in which the problem was found.
     * @param descriptor The descriptor for the problem that was found.
     */
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        showDetailsTooltip(element, project);
    }

    /**
     * This method is used to show a tooltip with the recommendation text and a link to the Azure SDK for Java documentation.
     *
     * @param element - the PsiElement where the tooltip should be shown
     * @param project - the Project object
     */  // separate class for UI configuration & separate styles.css file
    private void showDetailsTooltip(@NotNull PsiElement element, @NotNull Project project) {

        // Define dynamic text color based on theme
        JBColor dynamicTextColor = new JBColor(Gray._50, Gray._176);// Black for light, white for dark theme

        // Combine the recommendation text and the link on the same line
        String htmlContent = "<html><body style='color: " + toHex(dynamicTextColor) + ";'><div class='tooltip' role='tooltip' aria-live='polite'>" + recommendationText + " <a href='" + linkUrl + "' style='text-decoration: underline;'>Refer to Azure SDK for Java documentation</a>" + " for more information on this suggestion. </body></html>";

        // Create a panel for the tooltip
        JBPanel<JBPanel<?>> panel = QuickFixPanelConfigurations.createPanel(htmlContent);

        PsiFile psiFile = element.getContainingFile();
        Editor editor = EditorFactory.getInstance().getEditors(psiFile.getViewProvider().getDocument(), project)[0];

        // Get the relative position of the element in the editor
        RelativePoint relativePoint = new RelativePoint(editor.getContentComponent(), editor.visualPositionToXY(editor.getCaretModel().getVisualPosition()));

        // Create a balloon tooltip with custom appearance
        // This is specific to Java Swing/AWT components and IntelliJ's UI toolkit. This configuration is not directly related to CSS
        // thus, it is not possible to directly store this code snippet in the CSS file.
        JBPopupFactory.getInstance().createBalloonBuilder(panel).setFillColor(JBColor.background()) // Dynamic background color
                .setBorderColor(JBColor.border()) // Dynamic border color
                .setHideOnAction(true).setHideOnClickOutside(true).setHideOnFrameResize(true).setHideOnKeyOutside(true).setBorderInsets(new Insets(0, 0, 0, 0)) // Remove padding
                .createBalloon().show(relativePoint, Balloon.Position.above);
    }

    /**
     * This method is used to show a recommendation text with a link to the Azure SDK for Java documentation.
     *
     * @param recommendationText - the recommendation text to be shown in the tooltip
     * @param linkUrl            - the URL to be opened when the user clicks on the link in the tooltip
     * @return CustomTooltipOnHover object
     */
    static CustomTooltipOnHover showRecommendationText(String recommendationText, String linkUrl) {
        return new CustomTooltipOnHover(recommendationText, linkUrl);
    }
}
