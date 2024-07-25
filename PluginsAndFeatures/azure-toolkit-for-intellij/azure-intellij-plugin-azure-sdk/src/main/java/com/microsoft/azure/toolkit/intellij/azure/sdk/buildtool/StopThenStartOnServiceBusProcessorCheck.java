package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is a LocalInspectionTool that checks if a stop method is called on a ServiceBusProcessorClient object, followed by a start method call on the same object.
 * If this is the case, a problem is registered with the ProblemsHolder.
 */
public class StopThenStartOnServiceBusProcessorCheck extends LocalInspectionTool {

    /**
     * This method builds a visitor that visits the PsiMethodCallExpression and checks if a stop method is called on a ServiceBusProcessorClient object, followed by a start method call on the same object.
     * If this is the case, a problem is registered with the ProblemsHolder.
     *
     * @param holder     The ProblemsHolder to register the problem with
     * @param isOnTheFly A boolean that indicates if the inspection is being run on the fly - not used in this implementation but required by the method signature
     * @return A JavaElementVisitor that visits the PsiMethodCallExpression and checks if a stop method is called on a ServiceBusProcessorClient object, followed by a start method call on the same object
     */
    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new StopThenStartOnServiceBusProcessorVisitor(holder);
    }

    /**
     * This class is a JavaElementVisitor that visits the PsiMethodCallExpression and checks if a stop method is called on a ServiceBusProcessorClient object, followed by a start method call on the same object.
     * If this is the case, a problem is registered with the ProblemsHolder.
     */
    static class StopThenStartOnServiceBusProcessorVisitor extends JavaElementVisitor {

        // Create a ProblemsHolder to register the problem with
        private final ProblemsHolder holder;

        // Create a map to store a boolean indicating if stop was called on the variable
        private final Map<PsiVariable, Boolean> variableStateMap = new HashMap<>();

        // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        // Load the rule configuration
        static {
            final String ruleName = "StopThenStartOnServiceBusProcessorCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck() || RULE_CONFIG.getClientsToCheck().isEmpty();
        }

        /**
         * This constructor initializes the Visitor with the ProblemsHolder
         *
         * @param holder The ProblemsHolder to register the problem with
         */
        StopThenStartOnServiceBusProcessorVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        /**
         * This method visits the PsiMethodCallExpression and checks if a stop method is called on a ServiceBusProcessorClient object, followed by a start method call on the same object.
         * If this is the case, a problem is registered with the ProblemsHolder.
         *
         * @param expression The PsiMethodCallExpression to visit
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            if (SKIP_WHOLE_RULE) {
                return;
            }

            // Check if the method being called is 'stop' or 'start'
            PsiReferenceExpression methodExpression = expression.getMethodExpression();
            String methodName = methodExpression.getReferenceName();

            if (!(RULE_CONFIG.getMethodsToCheck().contains(methodName))) {
                return;
            }

            // Get the qualifier of the method call - the object on which the method is called
            PsiExpression qualifier = methodExpression.getQualifierExpression();

            if (!(qualifier instanceof PsiReferenceExpression)) {
                return;
            }
            PsiElement reference = ((PsiReferenceExpression) qualifier).resolve();

            if (!(reference instanceof PsiVariable)) {
                return;
            }

            // Get the variable that the method is called on
            PsiVariable variable = (PsiVariable) reference;

            // Check if the variable is a ServiceBusProcessorClient
            if (!(isServiceBusProcessorClient(variable))) {
                return;
            }

            // Boolean indicating if stop was called on the variable
            Boolean wasStopCalled = variableStateMap.get(variable);

            // If 'stop' is called, mark that 'stop' was called on the variable
            if ("stop".equals(methodName)) {
                variableStateMap.put(variable, true); // Mark that stop was called

                // If 'close' is called, remove the variable from the map -- the resource is closed and the variable is no longer in use
            } else if ("close".equals(methodName)) {
                variableStateMap.remove(variable); // Remove the variable from the map

                // If 'start' is called and 'stop' was called on the variable, register a problem
            } else if ("start".equals(methodName) && Boolean.TRUE.equals(wasStopCalled)) {
                holder.registerProblem(expression, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));

                // Reset the state after reporting the problem
                variableStateMap.remove(variable);
            }
        }

        /**
         * This method checks if the type of the variable is ServiceBusProcessorClient
         *
         * @param variable The variable to check
         * @return A boolean indicating if the type of the variable is ServiceBusProcessorClient
         */
        private static boolean isServiceBusProcessorClient(PsiVariable variable) {

            PsiType type = variable.getType();
            String typeText = type.getCanonicalText();
            return typeText != null && typeText.contains(RULE_CONFIG.getClientsToCheck().get(0)) && typeText.startsWith(RuleConfig.AZURE_PACKAGE_NAME);
        }
    }
}
