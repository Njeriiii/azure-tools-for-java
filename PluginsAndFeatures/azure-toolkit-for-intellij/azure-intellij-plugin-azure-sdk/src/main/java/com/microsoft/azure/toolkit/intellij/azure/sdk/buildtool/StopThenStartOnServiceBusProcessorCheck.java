package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StopThenStartOnServiceBusProcessorCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new StopThenStartOnServiceBusProcessorVisitor(holder);
    }


    static class StopThenStartOnServiceBusProcessorVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final Map<PsiVariable, Boolean> variableStateMap = new HashMap<>();

        // // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "StopThenStartOnServiceBusProcessorCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck() || RULE_CONFIG.getClientsToCheck().isEmpty();

            System.out.println("RULE_CONFIG: " + RULE_CONFIG);
            System.out.println("SKIP_WHOLE_RULE: " + SKIP_WHOLE_RULE);
            System.out.println("RULE_CONFIG.getMethodsToCheck(): " + RULE_CONFIG.getMethodsToCheck());
            System.out.println("RULE_CONFIG.getClientsToCheck(): " + RULE_CONFIG.getClientsToCheck());
            System.out.println("RULE_CONFIG.getAntiPatternMessageMap(): " + RULE_CONFIG.getAntiPatternMessageMap());
        }


        StopThenStartOnServiceBusProcessorVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }


        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);


            // Check if the method being called is 'stop' or 'start'
            PsiReferenceExpression methodExpression = expression.getMethodExpression();
            System.out.println("methodExpression: " + methodExpression);

            String methodName = methodExpression.getReferenceName();
            System.out.println("methodName: " + methodName);

            if (RULE_CONFIG.getMethodsToCheck().contains(methodName)) {
                PsiExpression qualifier = methodExpression.getQualifierExpression();
                System.out.println("qualifier: " + qualifier);

                if (qualifier instanceof PsiReferenceExpression) {
                    PsiElement reference = ((PsiReferenceExpression) qualifier).resolve();
                    System.out.println("reference: " + reference);

                    if (reference instanceof PsiVariable) {
                        PsiVariable variable = (PsiVariable) reference;
                        System.out.println("variable: " + variable);

                        // Check if the variable is a ServiceBusProcessorClient
                        if (isServiceBusProcessorClient(variable)) {
                            System.out.println("ServiceBusProcessorClient found");

                            if (isServiceBusProcessorClient(variable)) {
                                Boolean wasStopCalled = variableStateMap.get(variable);
                                System.out.println("wasStopCalled: " + wasStopCalled);

                                if ("stop".equals(methodName)) {
                                    System.out.println("stop called");
                                    variableStateMap.put(variable, true); // Mark that stop was called
                                    System.out.println("variableStateMapBeforeStop: " + variableStateMap);
                                } else if ("close".equals(methodName)) {
                                    System.out.println("close called");
                                    variableStateMap.remove(variable); // Remove the variable from the map
                                    System.out.println("variableStateMapBeforeClose: " + variableStateMap);

                                } else if ("start".equals(methodName) && Boolean.TRUE.equals(wasStopCalled)) {
                                    System.out.println("Problem is registered");
                                    System.out.println("variableStateMapBeforeProblemRegistered: " + variableStateMap);
                                    holder.registerProblem(expression, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));

                                    // Reset the state after reporting
                                    variableStateMap.remove(variable);
                                    System.out.println("variableStateMapAfterProblemRegistered: " + variableStateMap);
                                }
                            }
                        }
                    }
                }
            }
        }

        private boolean isServiceBusProcessorClient(PsiVariable variable) {
            // Check if the type of the variable is ServiceBusProcessorClient
            PsiType type = variable.getType();
            String typeText = type.getCanonicalText();
            System.out.println("typeText: " + typeText);
            System.out.println("RULE_CONFIG.getClientsToCheck().get(0): " + RULE_CONFIG.getClientsToCheck().get(0));
            System.out.println("type" + type);
            return typeText != null && typeText.contains(RULE_CONFIG.getClientsToCheck().get(0)) && typeText.startsWith(RuleConfig.AZURE_PACKAGE_NAME);
        }
    }
}
