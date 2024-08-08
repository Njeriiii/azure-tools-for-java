package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.replaceaction;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Inner class for quick-fix
public class ReplaceElementQuickFix implements LocalQuickFix {

    private final PsiElement discouragedElement;
    private final String suggestedElementText;

    public ReplaceElementQuickFix(PsiElement discouragedElement, String suggestedElementText) {
        this.discouragedElement = discouragedElement;
        this.suggestedElementText = suggestedElementText;
    }

    @Override
    public String getName() {
        if (suggestedElementText == " ") {
            return "Remove discouraged element";
        }
        return "Replace with " + suggestedElementText;
    }

    @Override
    public String getFamilyName() {
        return "Replace discouraged element usage";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getPsiElement();
        if (element != null && element.isValid()) {
            PsiElement suggestedElement = createElementFromText(project, suggestedElementText, element);
            if (suggestedElement != null && suggestedElement.isValid()) {
                discouragedElement.replace(suggestedElement);
            }
        }
    }

    @Nullable
    private PsiElement createElementFromText(Project project, String text, PsiElement context) {
        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
        PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);

        if (context instanceof XmlTag) {
            // Create a new XmlTag (if applicable)
            return createXmlTagFromText(project, text, (XmlTag) context);
        }

        if (text == " ") {
            // return a generic empty element
            PsiFile dummyFile = fileFactory.createFileFromText("dummy.java", context.getContainingFile().getFileType(), "");
            return dummyFile.getFirstChild();
        }

        return elementFactory.createExpressionFromText(text, context);
    }

    @Nullable
    private PsiElement createXmlTagFromText(Project project, String text, XmlTag context) {
        PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);
        PsiFile psiFile = fileFactory.createFileFromText("dummy.xml", context.getContainingFile().getFileType(), text);
        return psiFile.getFirstChild();
    }
}
