package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.source.tree.java.PsiDeclarationStatementImpl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ClosingCloseableClientsCheck extends LocalInspectionTool {

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Close Closeable Clients";
    }

    // list of closeable client interfaces
    private static final List<String> CLOSEABLE_CLASSES = Arrays.asList("java.io.Closeable", "java.lang.AutoCloseable", "org.apache.http.impl.client.CloseableHttpClient");


    // // This method is called for each file that is inspected. It returns a visitor that will visit each element in the file.
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        System.out.println("Checking for closeable clients");

        // print out the file/class name currently being checked
        System.out.println(holder.getFile().getName());

        return new JavaElementVisitor() {

            @Override
            public void visitLocalVariable(PsiLocalVariable variable) {

                // super.visitLocalVariable(variable) is called to ensure that the visitor will visit all the elements in the file
                super.visitLocalVariable(variable);

                System.out.println("Checking for new expressions");

                final PsiType type = variable.getType();
                final String qualifiedName = type.getCanonicalText();

                // print out the type of the variable
                System.out.println(qualifiedName);

                // print name of the variable
                System.out.println(variable.getName());

                // check if the variable is of type CloseableHttpClient
                if (CLOSEABLE_CLASSES.contains(qualifiedName)) {
                    System.out.println("CloseableHttpClient found");
                    checkClosed(variable, holder);
                }
            }

            private void checkClosed(PsiLocalVariable variable, @NotNull ProblemsHolder holder) {

                // check if the variable is being closed in a try-with-resources block

                System.out.println("Checking if CloseableHttpClient is being closed properly");

                // get the parent of the variable
                PsiElement parent = variable.getParent();
                System.out.println(parent.getClass().getName());
                while (parent != null) {

                    // get the resource list of the try statement
                    // this is the list of resources that are being closed in the try-with-resources block
                    // check if the parent is a try statement
                    if (parent instanceof PsiTryStatement) {
                        System.out.println("Parent is tryStatement");

                        PsiTryStatement tryStatement = (PsiTryStatement) parent;
                        System.out.println(tryStatement.getClass().getName());

                        PsiResourceList resourceList = tryStatement.getResourceList();

                        System.out.println("Printing resourceList");
                        System.out.println(resourceList);
                        if (resourceList != null) {
                            // check if the variable is in the resource list
                            for (PsiResourceListElement resource : resourceList) {

                                // if the variable is in the resource list, it means it is being closed in the try-with-resources block
                                if (resource.getOwnDeclarations() != variable) {
                                    System.out.println("CloseableHttpClient is not being closed properly");
                                    holder.registerProblem(variable, "CloseableHttpClient should be closed after use");;
                                } else {
                                    System.out.println("CloseableHttpClient is being closed properly");
                                }
                            }
                        }
                    }
                    parent = parent.getParent();
                }
            }
        };
    }
}
