package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.source.tree.java.PsiDeclarationStatementImpl;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ClosingCloseableClientsCheck extends LocalInspectionTool {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new CloseableClientVisitor(holder, isOnTheFly);
    }


    static class CloseableClientVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;

        CloseableClientVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * Visit a variable declaration statement
         * Check if it is a Closeable client and if it is properly closed
         *
         * @param variable
         */
        @Override
        public void visitVariable(PsiVariable variable) {
            System.out.println("Variable: " + variable);
            System.out.println("VariableName: " + variable.getName());
            System.out.println("VariableNameIdentifier: " + variable.getNameIdentifier());
            System.out.println("VariableText: " + variable.getText());

            PsiType type = variable.getType();
            System.out.println("Type: " + type);
            System.out.println("TypeCanonical: " + type.getCanonicalText());

            if (type instanceof PsiClassType) {
                PsiClass resolvedClass = ((PsiClassType) type).resolve();
                System.out.println("ResolvedClass: " + resolvedClass);
                if (resolvedClass != null && isAzureClient(resolvedClass)) {

                    System.out.println("AzureClient: " + resolvedClass);
                    PsiElement scope = variable.getParent();
                    System.out.println("scope: " + scope);
                    if (isCloseable(resolvedClass)) {
                        checkIfDeclaredInTryWith(variable);
                    }
                }
            }
        }

        private boolean isAzureClient(PsiClass psiClass) {
            String qualifiedName = psiClass.getQualifiedName();
            return qualifiedName != null && qualifiedName.startsWith("com.azure.");
        }

        private boolean isCloseable(PsiClass psiClass) {
            for (PsiClassType superType : psiClass.getSuperTypes()) {
                System.out.println("SuperType: " + superType);
                System.out.println("SuperType: " + superType.getCanonicalText());
                if (superType.equalsToText("java.lang.AutoCloseable")) {
                    System.out.println("We're heree");
                    return true;
                }
            }
            return false;
        }

        private void checkIfDeclaredInTryWith(PsiVariable variable) {
            boolean closed;
            boolean declaredInTryWithResources = false;

            // Get the parent element of the variable
            PsiElement parent = variable.getParent();
            System.out.println("parent: " + parent);

            if (parent instanceof PsiResourceList) {
                // The PsiResourceList's parent should be a PsiTryStatement
                PsiElement greatGrandParent = parent.getParent();
                System.out.println("greatGrandParent: " + greatGrandParent);

                if (greatGrandParent instanceof PsiTryStatement) {
                    declaredInTryWithResources = true;
                }
            }

            // parent of the variable is the declaration statement
            PsiElement parentBlock = variable.getParent().getParent();

            System.out.println("parent: " + parentBlock);
            System.out.println("declaredInTryWithResources " + declaredInTryWithResources);

            // If the variable is not declared in a try-with-resources block, check if it is closed in the code block
            if (!declaredInTryWithResources && parentBlock instanceof PsiCodeBlock) {
                closed = isResourceClosed(variable, (PsiCodeBlock) parentBlock);
            } else {
                closed = true; // Already handled by try-with-resources
            }

            if (!closed) {
                System.out.println("Variable is not closed");
                System.out.println("variable.getNameIdentifier(): " + variable.getNameIdentifier());
                holder.registerProblem(variable.getNameIdentifier(), "Closeable client is not properly closed");
            }
        }

        private boolean isResourceClosed(PsiVariable variable, PsiCodeBlock codeBlock) {
            System.out.println("isClose/DisposeCalledInStatements");

            for (PsiStatement statement : codeBlock.getStatements()) {
                System.out.println("statement: " + statement);
                if (statement instanceof PsiTryStatement) {
                    PsiTryStatement tryStatement = (PsiTryStatement) statement;
                    System.out.println("tryStatement: " + tryStatement);
                    PsiCodeBlock finallyBlock = tryStatement.getFinallyBlock();
                    System.out.println("finallyBlock: " + finallyBlock);
                    if (finallyBlock != null && findClosingMethodCall(finallyBlock, variable)) {
                        System.out.println("Variable is closed");
                        return true;
                    } else if (findClosingMethodCall(codeBlock.getContainingFile(), variable)) {
                        System.out.println("Variable is closed");
                        return true;
                    }
                }
            }
            return false;
        }



        static boolean findClosingMethodCall(PsiElement element, PsiVariable variable) {

            if (variable == null || element == null) {
                return false;
            }

            System.out.println("element: " + element);
            System.out.println("variable: " + variable);


            // Use an anonymous inner class to process method calls
            final boolean[] variableClosed = {false};

            element.accept(new JavaElementVisitor() {
                @Override
                public void visitMethodCallExpression(PsiMethodCallExpression methodCall) {
                    super.visitMethodCallExpression(methodCall);

                    PsiReferenceExpression methodExpression = methodCall.getMethodExpression();
                    System.out.println("methodExpression: " + methodExpression);
                    System.out.println("methodExpression.getReferenceName(): " + methodExpression.getReferenceName());

                    // Check if the method call is to the close method
                    if ("close".equals(methodExpression.getReferenceName())) {// && isClosingMethodCallExpression(methodExpression, variable)) {

                        PsiExpression qualifier = methodExpression.getQualifierExpression();
                        System.out.println("qualifier: " + qualifier);
                        System.out.println("((PsiReferenceExpression) qualifier).resolve() : " + ((PsiReferenceExpression) qualifier).resolve());
                        // Check if the qualifier is a reference expression and if it resolves to the variable
                        if (qualifier instanceof PsiReferenceExpression && ((PsiReferenceExpression) qualifier).resolve() == variable) {
                            System.out.println("Variable is closed");
                            variableClosed[0] = true;
                        }
                    }

                }
            });
            System.out.println("variableClosed[0]: " + variableClosed[0]);
            return variableClosed[0];
        }
    }
}