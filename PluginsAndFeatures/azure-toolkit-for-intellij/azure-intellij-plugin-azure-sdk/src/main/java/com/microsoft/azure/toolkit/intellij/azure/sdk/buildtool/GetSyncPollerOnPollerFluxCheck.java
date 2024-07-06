package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inspection tool to detect the use of getSyncPoller() on a PollerFlux.
 * The inspection will check if the method call is on a PollerFlux and if the method call is on an Azure SDK client.
 * If both conditions are met, the inspection will register a problem with the suggestion to use SyncPoller instead.
 *
 * This is an example of an anti-pattern that would be detected by the inspection tool.
 * public void exampleUsage() {
 *         PollerFlux<String> pollerFlux = createPollerFlux();
 *
 *         // Anti-pattern: Using getSyncPoller() on PollerFlux
 *         SyncPoller<String, Void> syncPoller = pollerFlux.getSyncPoller();
 *     }
 */
public class GetSyncPollerOnPollerFluxCheck extends LocalInspectionTool {

    /**
     * Method to build the visitor for the inspection tool.
     * @param holder Holder for the problems found by the inspection
     * @param isOnTheFly Flag to indicate if the inspection is running on the fly
     * @return JavaElementVisitor a visitor to visit the method call expressions
     */
    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new GetSyncPollerOnPollerFluxVisitor(holder, isOnTheFly);
    }

    /**
     * Visitor class to visit the method call expressions and check for the use of getSyncPoller() on a PollerFlux.
     * The visitor will check if the method call is on a PollerFlux and if the method call is on an Azure SDK client.
     */
    public static class GetSyncPollerOnPollerFluxVisitor extends JavaElementVisitor {

        // Instance variables
        private final ProblemsHolder holder;
        private final boolean isOnTheFly;

        private static String METHOD_TO_CHECK = "";
        private static String ANTI_PATTERN_MESSAGE = "";

        private static final Logger LOGGER = Logger.getLogger(GetSyncPollerOnPollerFluxCheck.class.getName());


        // Load the config file
        static {
            try {
                List<String> ruleConfigList = getRuleConfigs();

                // extract the method to check and the anti-pattern message from the config file
                METHOD_TO_CHECK = ruleConfigList.get(0);
                ANTI_PATTERN_MESSAGE = ruleConfigList.get(1);

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error loading rule data", e);
            }
        }

        /**
         * Constructor to initialize the visitor with the holder and isOnTheFly flag.
         * @param holder Holder for the problems found by the inspection
         * @param isOnTheFly Flag to indicate if the inspection is running on the fly
         */
        public GetSyncPollerOnPollerFluxVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        /**
         * Method to visit the method call expressions and check for the use of getSyncPoller() on a PollerFlux.
         * @param expression Method call expression to visit
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            // Check if the element is a method call expression
            if (!(expression instanceof PsiMethodCallExpression)) {
                return;
            }

            PsiMethodCallExpression methodCall = expression;

            // Check if the method call is getSyncPoller
            if (methodCall.getMethodExpression().getReferenceName().startsWith(METHOD_TO_CHECK)) {
                boolean isAsyncContext = checkIfAsyncContext(methodCall);

                if (isAsyncContext && isAzureClient(methodCall)) {
                    holder.registerProblem(expression, ANTI_PATTERN_MESSAGE);
                }
            }
        }

        /**
         * Helper method to check if the method call is within an async context.
         * This method will check if the method call is on a PollerFlux type.
         *
         * @param methodCall Method call expression to check
         * @return true if the method call is on a reactive type, false otherwise
         */
        private boolean checkIfAsyncContext(@NotNull PsiMethodCallExpression methodCall) {
            PsiExpression expression = methodCall.getMethodExpression().getQualifierExpression();

            // Check if the method call is on a reactive type
            if (expression == null) {
                return false;
            }
            PsiType type = expression.getType();

            // Check if the type is a reactive type
            if (type == null) {
                return false;
            }
            String typeName = type.getCanonicalText();

            // Check for PollerFlux type
            if (typeName != null && typeName.contains("PollerFlux")) {
                return true;
            }
            return false;
        }

        /**
         * Helper method to check if the method call is on an Azure SDK client.
         * This method will check if the method call is on a class that is part of the Azure SDK.
         * @param methodCall Method call expression to check
         * @return true if the method call is on an Azure SDK client, false otherwise
         */
        private boolean isAzureClient (@NotNull PsiMethodCallExpression methodCall){

            PsiClass containingClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);

            // Check if the method call is on a class
            if (containingClass == null) {
                return false;
            }
            String className = containingClass.getQualifiedName();

            // Check if the class is part of the Azure SDK
            if (className != null && className.startsWith("com.azure.")) {
                return true;
            }
        return false;
        }

        /**
         * Helper method to load the rule configurations from the config file.
         * @return List of strings containing the method to check and the anti-pattern message
         * @throws IOException if there is an error loading the config file
         */
        private static List<String> getRuleConfigs() throws IOException {

            String ruleName = "GetSyncPollerOnPollerFluxCheck";
            String antiPatternMessageKey = "antipattern_message";
            String methodToCheckKey = "method_to_check";

            //load json object
            JSONObject jsonObject = LoadJsonConfigFile.getInstance().getJsonObject();

            // extract string from json object
            String antiPatternMessage = jsonObject.getJSONObject(ruleName).getString(antiPatternMessageKey);
            String methodToCheck = jsonObject.getJSONObject(ruleName).getString(methodToCheckKey);

            // return list of strings
            return List.of(methodToCheck, antiPatternMessage);
        }
    }
}