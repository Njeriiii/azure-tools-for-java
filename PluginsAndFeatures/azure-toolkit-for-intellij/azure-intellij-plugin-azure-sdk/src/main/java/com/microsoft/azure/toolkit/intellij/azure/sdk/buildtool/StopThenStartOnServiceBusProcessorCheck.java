package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiParameter;
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
        private final Map<Integer, Boolean> variableStateMap = new HashMap<>();

        // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        PsiVariable associatedVariable = null;

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
         * @param element
         */
        @Override
        public void visitElement(@NotNull PsiElement element) {
            super.visitElement(element);

            System.out.println("Start of visitElement");

            // check if there's been a new expression
            if (SKIP_WHOLE_RULE) {
                return;
            }

            if (element instanceof PsiNewExpression) {
                PsiNewExpression newExpression = (PsiNewExpression) element;
                System.out.println("New Expression: " + newExpression);

                PsiVariable tempVariable = findAssociatedVariable(newExpression);
                System.out.println("Associated Variable: " + tempVariable);

                if (tempVariable != null && isServiceBusProcessorClient(tempVariable)) {
                    associatedVariable = tempVariable;
                    variableStateMap.put(associatedVariable.hashCode(), false);
                    System.out.println("Associated Variable: " + associatedVariable.getName());
                    System.out.println("AssociatedVariable.hashCode(): " + associatedVariable.hashCode());
                    System.out.println("Variable State Map: " + variableStateMap);
                } else {
                    System.out.println("No associated variable found.");
                }
            }

            // if a method is found, check its body
            if (element instanceof PsiMethod) {
                PsiMethod method = (PsiMethod) element;
                System.out.println("Method: " + method);

                PsiCodeBlock body = method.getBody();
                System.out.println("Body: " + body);

                boolean stopStartInMethodBody = false;

                if (body != null) {
                    visitMethodBody(body, stopStartInMethodBody);  // Start visiting the body elements
                }

                System.out.println("We are done with the method body");
                System.out.println("Variable State Map: " + variableStateMap);
                System.out.println("Associated Variable: " + associatedVariable);

                if (associatedVariable != null) {
                    variableStateMap.put(associatedVariable.hashCode(), false);
                    System.out.println("Variable State Map putting back false: " + variableStateMap);
                }
            }
            System.out.println("End of visitElement");
        }

        // Recursively visit all elements in a method body
        private void visitMethodBody(@NotNull PsiCodeBlock body, boolean stopStartInMethodBody) {

            System.out.println("stopStartInMethodBody: " + stopStartInMethodBody);

            for (PsiElement child : body.getChildren()) {

                System.out.println("Child: " + child);

                // If the child is a method call, resolve it
                if (child instanceof PsiExpressionStatement) {

                    System.out.println("Expression Statement: " + child);

                    PsiExpression expression = ((PsiExpressionStatement) child).getExpression();
                    System.out.println("Expression: " + expression);

                    if (!(expression instanceof PsiMethodCallExpression)) {
                        continue;
                    }

                    // Handle the method call
                    PsiMethodCallExpression methodCall = (PsiMethodCallExpression) expression;
                    System.out.println("Method Call: " + methodCall);

                    // check the element being called on
                    PsiElement element = methodCall.getMethodExpression().getQualifierExpression();
                    System.out.println("Element: " + element);

                    if (element instanceof PsiReferenceExpression) {
                        PsiReferenceExpression reference = (PsiReferenceExpression) element;
                        System.out.println("Reference: " + reference);

                        PsiElement resolvedElement = reference.resolve();
                        System.out.println("Resolved Element: " + resolvedElement);
                        System.out.println("resolvedElement.hashCode(): " + resolvedElement.hashCode());

                        // Check if the resolved element is a variable
                        if ((resolvedElement instanceof PsiVariable)) {
                            PsiVariable variable = (PsiVariable) resolvedElement;
                            System.out.println("Variable: " + variable);
                            System.out.println("Variable Name: " + variable.getName());
                            System.out.println("Variable Type: " + variable.getType());
                            System.out.println("Variable getNameIdentifier: " + variable.getNameIdentifier());
                            System.out.println("Variable getParent: " + variable.getParent());
                            System.out.println("Variable getIdentifyingElement: " + variable.getIdentifyingElement());
                            System.out.println("Variable State Map: " + variableStateMap);

                            if (variableStateMap.containsKey(resolvedElement.hashCode())) {
                                if (checkMethodCall(methodCall, stopStartInMethodBody)) {
                                    System.out.println("Registering Problem");
                                    holder.registerProblem(methodCall, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
                                }
                            }
                        }
                    }

                    // Resolve the method call to its method definition
                    PsiMethod resolvedMethod = methodCall.resolveMethod();

                    if (resolvedMethod != null) {
                        System.out.println("Resolved Method: " + resolvedMethod);

                        PsiFile containingFile = resolvedMethod.getContainingFile();
                        if (containingFile != null) {
                            // Check if the containing file is within the current project
                            boolean isInCurrentProject = isFileInCurrentProject(containingFile);
                            System.out.println("Is In Current Project: " + isInCurrentProject);

                            if (isInCurrentProject) {
                                PsiCodeBlock resolvedBody = resolvedMethod.getBody();
                                System.out.println("Resolved Body: " + resolvedBody);

                                if (resolvedBody != null) {
                                    visitMethodBody(resolvedBody, stopStartInMethodBody);  // Recursively visit the resolved method's body
                                }
                            }
                        }
                    }
                }

                // Continue visiting children of the current element
//                child.accept(this);
            }
        }

        // Helper method to check if the file is in the current project
        private boolean isFileInCurrentProject(PsiFile file) {
            // Assuming `project` is an instance of com.intellij.openapi.project.Project
            Project project = file.getProject();
            System.out.println("Project: " + project);
            return ProjectRootManager.getInstance(project).getFileIndex().isInContent(file.getVirtualFile());
        }


        /**
         * This method visits the PsiMethodCallExpression and checks if a stop method is called on a ServiceBusProcessorClient object, followed by a start method call on the same object.
         * If this is the case, a problem is registered with the ProblemsHolder.
         *
         * @param expression The PsiMethodCallExpression to visit
         */
        private boolean checkMethodCall(PsiMethodCallExpression expression, boolean stopStartInMethodBody) {

            System.out.println("checkMethodCall");
            System.out.println("stopStartInMethodBody: " + stopStartInMethodBody);

            // Check if the method being called is 'stop' or 'start'
            PsiReferenceExpression methodExpression = expression.getMethodExpression();
            String methodName = methodExpression.getReferenceName();

            System.out.println("Method Name: " + methodName);

            System.out.println("Methods to Check: " + RULE_CONFIG.getMethodsToCheck());

            if (!(RULE_CONFIG.getMethodsToCheck().contains(methodName))) {
                return stopStartInMethodBody;
            }

            // Get the qualifier of the method call - the object on which the method is called
            PsiExpression qualifier = methodExpression.getQualifierExpression();
            System.out.println("Qualifier: " + qualifier);

            if (!(qualifier instanceof PsiReferenceExpression)) {
                return stopStartInMethodBody;
            }
            PsiElement reference = ((PsiReferenceExpression) qualifier).resolve();
            System.out.println("Reference: " + reference);

//            if (!(reference instanceof PsiVariable)) {
//                return;
//            }

            // Get the variable that the method is called on
            PsiVariable variable = (PsiVariable) reference;
            System.out.println("VariableInCheckMethod: " + variable);

            // Boolean indicating if stop was called on the variable
            Boolean wasStopCalled = variableStateMap.get(variable.hashCode());
            System.out.println("Was Stop Called: " + wasStopCalled);

            // If 'stop' is called, mark that 'stop' was called on the variable
            if ("stop".equals(methodName)) {
                System.out.println("Stop Method Called");
                System.out.println("Variable Hash Code: " + variable.hashCode());
                variableStateMap.put(variable.hashCode(), true); // Mark that stop was called

                // If 'start' is called and 'stop' was called on the variable, register a problem
            } else if ("start".equals(methodName)) {
                if (wasStopCalled) {
                    stopStartInMethodBody = true;
                    System.out.println("Start Method Called");
                    return stopStartInMethodBody;
                }

//                    && Boolean.TRUE.equals(wasStopCalled)) {
//                holder.registerProblem(methodExpression, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
//
//                // Reset the state after reporting the problem
//                variableStateMap.remove(variable);
            }
            return false;
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

        /**
         * Finds the variable associated with a PsiNewExpression.
         *
         * @param newExpression The PsiNewExpression instance.
         * @return The associated PsiVariable or null if not found.
         */
        public PsiVariable findAssociatedVariable(PsiNewExpression newExpression) {
            System.out.println("New Expression: " + newExpression);

            PsiElement parent = newExpression.getParent();

            // Traverse upwards to find a variable declaration or assignment
            while (parent != null) {
                if (parent instanceof PsiVariable) {
                    return (PsiVariable) parent; // Direct variable initialization
                } else if (parent instanceof PsiAssignmentExpression) {
                    PsiAssignmentExpression assignmentExpression = (PsiAssignmentExpression) parent;
                    System.out.println("Assignment Expression: " + assignmentExpression);

                    PsiExpression lhs = assignmentExpression.getLExpression();
                    System.out.println("LHS: " + lhs);

                    if (lhs instanceof PsiReferenceExpression) {
                        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) lhs;
                        System.out.println("Reference Expression: " + referenceExpression);

                        PsiElement resolvedElement = referenceExpression.resolve();
                        System.out.println("Resolved Element: " + resolvedElement);

                        if (resolvedElement instanceof PsiVariable) {
                            return (PsiVariable) resolvedElement; // Variable in assignment
                        }
                    }
                }
                // Move up the tree
                parent = parent.getParent();
            }
            return null; // No associated variable found
        }
    }
}


/// NOTES
// Check within the same method body for both start AND stop -- if there's only one, don't track it
