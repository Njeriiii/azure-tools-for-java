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
        return new CloseableClientVisitor(holder);
    }


    class CloseableClientVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;

        public CloseableClientVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

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

                    if (scope instanceof PsiDeclarationStatement) {
                        PsiElement context = scope.getParent();
                        System.out.println("context: " + context);
                        if (context instanceof PsiCodeBlock) {
                            System.out.println("contextPsiCodeBlock: " + context);

                            String implementation = checkImplementation(resolvedClass);
                            if (implementation == null) {
                                return;
                            }

                            if (implementation.equals("Closeable")) {
                                System.out.println("Closeable");
                                checkIfClosedInCodeBlock(variable, (PsiCodeBlock) context);
                            } else if (implementation.equals("Disposable") && isResourceClosedOrDisposedOf((PsiCodeBlock) context, variable, "Disposable")) {
                                System.out.println("Disposable");
                                holder.registerProblem(variable.getNameIdentifier(), "Disposable subscription is not properly disposed of");
                            }
                        }
                    }
                }
            }
        }

        private boolean isAzureClient(PsiClass psiClass) {
            String qualifiedName = psiClass.getQualifiedName();
            return qualifiedName != null && qualifiedName.startsWith("com.azure.");
        }

        private String checkImplementation(PsiClass psiClass) {
            for (PsiClassType superType : psiClass.getSuperTypes()) {
                System.out.println("SuperType: " + superType);
                System.out.println("SuperType: " + superType.getCanonicalText());
                if (superType.equalsToText("java.lang.AutoCloseable")) {
                    return "Closeable";
                }
                if (superType.equalsToText("reactor.core.Disposable")) {
                    return "Disposable";
                }
            }
            return null;
        }

        private void checkIfClosedInCodeBlock(PsiVariable variable, PsiCodeBlock codeBlock) {
            boolean closed;
            boolean declaredInTryWithResources = false;

            System.out.println("codeBlock: " + codeBlock);

            PsiElement parent = variable.getParent().getParent();
            System.out.println("parent: " + parent);

            if (parent instanceof PsiTryStatement) {
                PsiTryStatement tryStatement = (PsiTryStatement) parent;
                System.out.println("tryStatement: " + tryStatement);

                PsiResourceList resourceList = tryStatement.getResourceList();
                System.out.println("resourceList: " + resourceList);

                if (resourceList != null) {
                    for (PsiElement resource : resourceList) {
                        System.out.println("resource: " + resource);
                        if (resource instanceof PsiResourceVariable && ((PsiResourceVariable) resource).getNameIdentifier().equals(variable.getNameIdentifier())) {
                            declaredInTryWithResources = true;
                            System.out.println("declaredInTryWithResources: " + declaredInTryWithResources);
                        }
                    }
                }
            }

            // If the variable is not declared in a try-with-resources block, check if it is closed in the code block
            if (!declaredInTryWithResources) {
                String resourceType = "Closeable";
                closed = isResourceClosedOrDisposedOf((PsiCodeBlock) parent, variable, resourceType);
            } else {
                closed = true; // Already handled by try-with-resources
            }

            if (!closed) {
                holder.registerProblem(variable.getNameIdentifier(), "Closeable client is not properly closed");
            }
        }

        private boolean isResourceClosedOrDisposedOf(PsiCodeBlock codeBlock, PsiVariable variable, String resourceType) {
            System.out.println("isClose/DisposeCalledInStatements");

            for (PsiStatement statement : codeBlock.getStatements()) {
                System.out.println("statement: " + statement);
                if (statement instanceof PsiTryStatement) {
                    PsiTryStatement tryStatement = (PsiTryStatement) statement;
                    System.out.println("tryStatement: " + tryStatement);
                    PsiCodeBlock finallyBlock = tryStatement.getFinallyBlock();
                    System.out.println("finallyBlock: " + finallyBlock);
                    if (finallyBlock != null && processCodeBlock(finallyBlock, variable)) {
                        System.out.println("Variable is closed");
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean processCodeBlock(PsiCodeBlock finallyBlock, PsiVariable variable) {

            if (variable == null || finallyBlock == null) {
                return false;
            }

            // Use PsiTreeUtil to find all the method calls in the code block
            Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfType(finallyBlock, PsiMethodCallExpression.class);

            // Iterate through all the method calls
            for (PsiMethodCallExpression methodCall : methodCalls) {

                PsiReferenceExpression methodExpression = methodCall.getMethodExpression();
                System.out.println("methodExpression: " + methodExpression);

                // Check if the method call is to the close method
                if ("close".equals(methodExpression.getReferenceName()) && isClosingOrDisposingMethodCallExpression(methodExpression, variable)) {
                    System.out.println("Variable is closed");
                    return true;
                } else if ("dispose".equals(methodExpression.getReferenceName()) && isClosingOrDisposingMethodCallExpression(methodExpression, variable)) {
                    System.out.println("Variable is disposed of");
                    return true;   // Check if the method call is to the dispose method
                }
            }
            return false;
        }

        private boolean isClosingOrDisposingMethodCallExpression(PsiReferenceExpression methodExpression, PsiVariable variable) {

            // Get the qualifier expression of the method call
            PsiExpression qualifier = methodExpression.getQualifierExpression();
            System.out.println("qualifier: " + qualifier);
            System.out.println("((PsiReferenceExpression) qualifier).resolve() : " + ((PsiReferenceExpression) qualifier).resolve());

            // Check if the qualifier is a reference expression and if it resolves to the variable
            if (qualifier instanceof PsiReferenceExpression && ((PsiReferenceExpression) qualifier).resolve() == variable) {
                System.out.println("Variable is disposed of");
                return true;
            }
            return false;
        }
    }
}