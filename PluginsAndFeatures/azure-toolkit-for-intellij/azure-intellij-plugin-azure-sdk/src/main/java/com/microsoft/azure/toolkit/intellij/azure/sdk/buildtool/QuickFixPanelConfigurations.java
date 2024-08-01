package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.ide.BrowserUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * This class is used to create a panel with a specific HTML content.
 * The HTML content is styled using the styles.css file.
 * One of the uses of this class is to create a tooltip with a recommendation text and a link to the Azure SDK for Java documentation.
 */
class QuickFixPanelConfigurations {

    private static final Logger LOGGER = Logger.getLogger(QuickFixPanelConfigurations.class.getName());

    /**
     * This method is used to create a panel with a specific HTML content.
     * The HTML content is styled using the styles.css file.
     *
     * @param htmlContent - the HTML content to be shown in the panel
     * @return A panel with the specified HTML content
     */
    public static JBPanel<JBPanel<?>> createPanel(String htmlContent) {
        // Create a panel for the tooltip
        JBPanel<JBPanel<?>> panel = new JBPanel<>();
        panel.setBorder(JBUI.Borders.empty(5)); // Add padding

        // Create a JEditorPane for both the recommendation text and the link
        JEditorPane editorPane = new JEditorPane();
        HTMLEditorKit editorKit = new HTMLEditorKit();
        editorPane.setEditorKit(editorKit);
        StyleSheet styleSheet = editorKit.getStyleSheet();

        // Load the CSS from the styles.css file
        try (InputStream cssStream = QuickFixPanelConfigurations.class.getResourceAsStream("/META-INF/styles/styles.css")) {

            // Check if the CSS file is found
            if (cssStream == null) {
                throw new FileNotFoundException("CSS file not found.");
            }
            // Read the CSS file
            String css = new String(cssStream.readAllBytes());
            styleSheet.addRule(css);
        } catch (IOException e) {
            LOGGER.warning("Failed to load CSS file: " + e);
        }

        // Apply a font stack that's likely to match IntelliJ's appearance
        editorPane.setContentType("text/html");

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

        return panel;
    }
}
