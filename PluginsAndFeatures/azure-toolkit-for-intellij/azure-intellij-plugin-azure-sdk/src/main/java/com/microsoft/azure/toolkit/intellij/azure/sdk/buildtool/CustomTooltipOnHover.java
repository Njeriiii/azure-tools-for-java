package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.openapi.editor.EditorFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.JEditorPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * This class is used to create a custom quick fix that shows a tooltip with a recommendation text and a link to the Azure SDK for Java documentation.
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
     */
    private void showDetailsTooltip(@NotNull PsiElement element, @NotNull Project project) {

        // Create a panel for the tooltip
        JBPanel<JBPanel<?>> panel = new JBPanel<>();
        panel.setBorder(JBUI.Borders.empty(5)); // Add padding

        // Create a JEditorPane for both the recommendation text and the link
        JEditorPane editorPane = new JEditorPane();
        HTMLEditorKit editorKit = new HTMLEditorKit();
        editorPane.setEditorKit(editorKit);
        StyleSheet styleSheet = (editorKit).getStyleSheet();

        // Get IntelliJ's default editor font
        Font editorFont = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);
        int fontSize = editorFont.getSize();

        // Apply a font stack that's likely to match IntelliJ's appearance
        styleSheet.addRule("body { font-family: 'sans-serif'; font-size: " + fontSize + "pt; margin: 0; padding: 0; color: #FFFFFF; }");
        styleSheet.addRule("a { color: #8AC3F7; text-decoration: none; }");
        editorPane.setContentType("text/html");

        // Combine the recommendation text and the link on the same line
        String htmlContent = "<html><body style='width: 500px; color: #B0B0B0;'>" + recommendationText + " <a href='" + linkUrl + "'>Refer to Azure SDK for Java documentation</a>" + " for more information on this suggestion. </body></html>";

        // Set the HTML content to the editorPane
        editorPane.setText(htmlContent);
        editorPane.setEditable(false);
        editorPane.setBackground(panel.getBackground());
        editorPane.setForeground(JBColor.LIGHT_GRAY);

        // Add hyperlink listener
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(e.getURL());
            }
        });

        // Add the linkLabel to the panel
        panel.add(editorPane, BorderLayout.CENTER);

        PsiFile psiFile = element.getContainingFile();
        Editor editor = EditorFactory.getInstance().getEditors(psiFile.getViewProvider().getDocument(), project)[0];

        // Get the relative position of the element in the editor
        RelativePoint relativePoint = new RelativePoint(editor.getContentComponent(), editor.visualPositionToXY(editor.getCaretModel().getVisualPosition()));

        // Create a balloon tooltip with custom appearance
        JBPopupFactory.getInstance().createBalloonBuilder(panel).setFillColor(new Color(100, 100, 100)) // Dark gray background
                .setBorderColor(new Color(100, 100, 100)) // Remove border
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
