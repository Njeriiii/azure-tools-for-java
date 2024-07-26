package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiResourceList;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;

/**
 * This class contains the inspection tool for checking if Closeable clients are properly closed.
 * It checks if the Closeable client is declared in a try-with-resources block or if it is closed in the code block.
 * If the Closeable client is not closed, it registers a problem with the ProblemsHolder.
 */
public class ClosingCloseableClientsCheck extends LocalInspectionTool {

    /**
     * This method builds the visitor for the inspection tool.
     * It creates a new CloseableClientVisitor with the ProblemsHolder and isOnTheFly.
     */
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new CloseableClientVisitor(holder);
    }

    /**
     * This class contains the visitor for the inspection tool.
     * It visits the variable declaration statement and checks if it is a Closeable client.
     * If it is a Closeable client, it checks if it is properly closed.
     */
    static class CloseableClientVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        /**
         * Constructor for CloseableClientVisitor.
         * It initializes the ProblemsHolder.
         */
        CloseableClientVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        static {
            final String ruleName = "ClosingCloseableClientsCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck() || RULE_CONFIG.getListedItemsToCheck().isEmpty();
        }

        /**
         * This method visits the variable declaration statement and checks if it is a Closeable client.
         * If it is a Closeable client, it checks if it is properly closed.
         */
        @Override
        public void visitVariable(PsiVariable variable) {

            if (SKIP_WHOLE_RULE) {
                return;
            }
            PsiType type = variable.getType();

            if (type instanceof PsiClassType) {
                PsiClass resolvedClass = ((PsiClassType) type).resolve();

                // Check if the class is an Azure client implementing or extending Closeable
                if (resolvedClass != null && isAzureClient(resolvedClass)) {

                    // Check if its a closeable client
                    if (isCloseable(resolvedClass)) {

                        // Check if the variable is declared in a try-with-resources block
                        checkIfDeclaredInTryWith(variable);
                    }
                }
            }
        }

        /**
         * This method checks if the class is an Azure client.
         * It checks if the class is in the com.azure package.
         */
        private static boolean isAzureClient(PsiClass psiClass) {
            String qualifiedName = psiClass.getQualifiedName();
            return qualifiedName != null && qualifiedName.startsWith(RuleConfig.AZURE_PACKAGE_NAME);
        }

        /**
         * This method checks if the class is a Closeable client.
         * It checks if the class implements or extends Closeable.
         */
        private static boolean isCloseable(PsiClass psiClass) {
            for (PsiClassType superType : psiClass.getSuperTypes()) {

                if (RULE_CONFIG.getListedItemsToCheck().contains(superType.getCanonicalText())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * This method checks if the variable is declared in a try-with-resources block.
         * If the variable is not declared in a try-with-resources block, it checks if it is closed elsewhere in the source code.
         */
        private void checkIfDeclaredInTryWith(PsiVariable variable) {

            boolean closed;
            boolean declaredInTryWithResources = false;

            PsiElement parent = variable.getParent();

            if (parent instanceof PsiResourceList) {

                // If the grandParent of the variable is a PsiTryStatement, the variable is declared in a try-with-resources block
                PsiElement greatGrandParent = parent.getParent();

                if (greatGrandParent instanceof PsiTryStatement) {
                    declaredInTryWithResources = true;
                }
            }

            // parentBlock of the variable is the declaration statement
            PsiElement parentBlock = variable.getParent().getParent();

            // If the variable is not declared in a try-with-resources block, check if it is closed in a finally block
            if (!declaredInTryWithResources) {
                closed = isResourceClosed(variable, parentBlock);
            } else {
                closed = true; // Already handled by try-with-resources
            }

            if (!closed && variable.getNameIdentifier() != null) {
                holder.registerProblem(variable.getNameIdentifier(), RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
            }
        }

        /**
         * This method checks if the variable is closed elsewhere in the source code.
         * It checks if the variable is closed in the finally block of a try statement or if it is closed elsewhere in the source code.
         */
        private static boolean isResourceClosed(PsiVariable variable, PsiElement element) {

            if (element instanceof PsiCodeBlock) {
                PsiCodeBlock codeBlock = (PsiCodeBlock) element;

                // Check if the variable is closed in the finally block of a try statement
                for (PsiStatement statement : codeBlock.getStatements()) {

                    if (statement instanceof PsiTryStatement) {
                        PsiTryStatement tryStatement = (PsiTryStatement) statement;
                        PsiCodeBlock finallyBlock = tryStatement.getFinallyBlock();

                        // Look for 'close' method call in the finally block
                        if (finallyBlock != null && findClosingMethodCall(finallyBlock, variable)) {
                            return true;
                        }
                    }
                }
            }

            // If the variable is not closed in the finally block, check if it is closed elsewhere in the source code
            if (findClosingMethodCall(element.getContainingFile(), variable)) {
                return true;
            }
            return false;
        }


        /**
         * This method visits method call expressions to find a 'close' method call on the closeable client.
         * It checks if the method call is 'close' and if the qualifier is the closeable client.
         * If the close method call is found, it returns true.
         */
        private static boolean findClosingMethodCall(PsiElement element, PsiVariable variable) {

            if (variable == null || element == null) {
                return false;
            }

            // Use an anonymous inner class to process method calls
            final boolean variableClosed[] = {false};

            // Visit the method call expressions in the element
            element.accept(new JavaRecursiveElementVisitor() {
                @Override
                public void visitMethodCallExpression(PsiMethodCallExpression methodCall) {
                    super.visitMethodCallExpression(methodCall);

                    PsiReferenceExpression methodExpression = methodCall.getMethodExpression();

                    // Check if the method call is to the close method
                    if (RULE_CONFIG.getMethodsToCheck().get(0).equals(methodExpression.getReferenceName())) {

                        // Check if the qualifier is the closeable client variable
                        PsiExpression qualifier = methodExpression.getQualifierExpression();

                        // Check if the qualifier is a reference expression and if it resolves to the variable
                        if (qualifier instanceof PsiReferenceExpression && ((PsiReferenceExpression) qualifier).resolve() == variable) {
                            variableClosed[0] = true;
                        }
                    }

                }
            });
            return variableClosed[0];
        }
    }
}
