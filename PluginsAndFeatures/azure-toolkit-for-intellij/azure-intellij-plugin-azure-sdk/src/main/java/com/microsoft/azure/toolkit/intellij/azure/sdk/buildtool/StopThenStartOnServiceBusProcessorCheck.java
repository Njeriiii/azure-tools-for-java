package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
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

        // Create a variable to store the associated variable -- the variable that the method is called on
        private static PsiVariable associatedVariable = null;

        // Create a map to store the problems and their line numbers -- to avoid duplicate problems
        private static final Map<Integer, String> problemsMap = new HashMap<>();

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
         * @param holder The ProblemsHolder to register the problem with when found
         */
        StopThenStartOnServiceBusProcessorVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        /**
         * This method visits the PsiElement and checks if it's a declaration of a ServiceBusProcessorClient object.
         * If it is, the associated variable is stored in the variableStateMap.
         * The method also checks the body of the method for method calls and their definitions.
         * If a stop method is called on a ServiceBusProcessorClient object, followed by a start method call on the same object, a problem is registered with the ProblemsHolder.
         * The method also resets the associated variable to false after visiting the method body once the recursive calls are done.
         *
         * @param element The PsiElement to visit
         */
        @Override
        public void visitElement(@NotNull PsiElement element) {
            super.visitElement(element);

            // check if there's been a new expression
            if (SKIP_WHOLE_RULE) {
                return;
            }

            // Check if the element is a declaration of a ServiceBusProcessorClient object
            if (element instanceof PsiNewExpression) {
                PsiNewExpression newExpression = (PsiNewExpression) element;

                PsiVariable tempVariable = findAssociatedVariable(newExpression);

                if (tempVariable != null && isServiceBusProcessorClient(tempVariable)) {
                    associatedVariable = tempVariable;
                    variableStateMap.put(associatedVariable.hashCode(), false);
                }
            }


            // Check the body of the method for method calls and their definitions
            if (element instanceof PsiMethod && associatedVariable != null) {
                PsiMethod method = (PsiMethod) element;
                PsiCodeBlock body = method.getBody();

                if (body != null) {

                    // Start visiting the body elements
                    visitMethodBody(body);
                }

                // Reset the associated variable to false after visiting the method body
                if (associatedVariable != null) {
                    variableStateMap.put(associatedVariable.hashCode(), false);
                }
            }
        }

        /**
         * This method recursively visits all method calls and their definitions in the PsiCodeBlock.
         * It checks if a stop method is called on a ServiceBusProcessorClient object, followed by a start method call on the same object.
         * If this is the case, a problem is registered with the ProblemsHolder.
         *
         * @param body The PsiCodeBlock to visit
         */
        private void visitMethodBody(@NotNull PsiCodeBlock body) {

            // Iterate over the children of the method body to find method calls
            for (PsiElement child : body.getChildren()) {

                // Only process expression statements - they are Java statements that end in a semicolon
                if (!(child instanceof PsiExpressionStatement)) {
                    continue;
                }

                // Get the expression from the statement
                PsiExpression expression = ((PsiExpressionStatement) child).getExpression();

                // Only process method calls
                if (!(expression instanceof PsiMethodCallExpression)) {
                    continue;
                }

                // Handle the method call
                PsiMethodCallExpression methodCall = (PsiMethodCallExpression) expression;

                // check the element being called on -- if it's a variable, check if it's the one we're tracking
                PsiElement element = methodCall.getMethodExpression().getQualifierExpression();

                if (element instanceof PsiReferenceExpression) {

                    PsiReferenceExpression reference = (PsiReferenceExpression) element;
                    PsiElement resolvedElement = reference.resolve();

                    // Check if the resolved element is a variable
                    if ((resolvedElement instanceof PsiVariable)) {

                        if (variableStateMap.containsKey(resolvedElement.hashCode())) {

                            // Check the API calls on the variable we're tracking for a stop then start
                            if (checkMethodCall(methodCall)) {

                                // Get the containing file
                                PsiFile psiFile = element.getContainingFile();

                                // Get the project from the PsiFile
                                Project project = psiFile.getProject();

                                // Get the document corresponding to the PsiFile
                                Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);

                                if (document != null) {
                                    // Get the offset of the element
                                    int offset = element.getTextOffset();

                                    // Get the line number corresponding to the offset
                                    int lineNumber = document.getLineNumber(offset);

                                    if (!problemsMap.containsKey(lineNumber)) {
                                        holder.registerProblem(methodCall, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
                                        problemsMap.put(lineNumber, methodCall.getText());
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }

                // Resolve the method call to its method definition
                PsiMethod resolvedMethod = methodCall.resolveMethod();

                if (resolvedMethod == null) {
                    continue;
                }

                PsiFile containingFile = resolvedMethod.getContainingFile();
                if (containingFile == null) {
                    continue;
                }

                // Check if the containing file is within the current project
                boolean isInCurrentProject = isFileInCurrentProject(containingFile);
                if (isInCurrentProject) {
                    PsiCodeBlock resolvedBody = resolvedMethod.getBody();
                    if (resolvedBody != null) {
                        visitMethodBody(resolvedBody);  // Recursively visit the resolved method's body
                    }
                }
            }
        }


        /**
         * This method visits the PsiMethodCallExpression and checks if a stop method is called on a ServiceBusProcessorClient object, followed by a start method call on the same object.
         * If this is the case, a problem is registered with the ProblemsHolder.
         *
         * @param expression The PsiMethodCallExpression to visit
         */
        private boolean checkMethodCall(PsiMethodCallExpression expression) {

            // Check if the method being called is 'stop' or 'start'
            PsiReferenceExpression methodExpression = expression.getMethodExpression();
            String methodName = methodExpression.getReferenceName();

            if (!(RULE_CONFIG.getMethodsToCheck().contains(methodName))) {
                return false;
            }

            // Get the qualifier of the method call - the object on which the method is called
            PsiExpression qualifier = methodExpression.getQualifierExpression();

            if (!(qualifier instanceof PsiReferenceExpression)) {
                return false;
            }
            PsiElement reference = ((PsiReferenceExpression) qualifier).resolve();

            if (!(reference instanceof PsiVariable)) {
                return false;
            }

            // Get the variable that the method is called on
            PsiVariable variable = (PsiVariable) reference;

            // Boolean indicating if stop was called on the variable
            Boolean wasStopCalled = variableStateMap.get(variable.hashCode());

            // If 'stop' is called, mark that 'stop' was called on the variable
            if ("stop".equals(methodName)) {
                variableStateMap.put(variable.hashCode(), true); // Mark that stop was called

                // If 'start' is called and 'stop' was called on the variable, register a problem
            } else if ("start".equals(methodName)) {
                return wasStopCalled;
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
         * This method finds the variable associated with a PsiNewExpression.
         *
         * @param newExpression The PsiNewExpression instance.
         * @return The associated PsiVariable or null if not found.
         */
        public PsiVariable findAssociatedVariable(PsiNewExpression newExpression) {

            // Get the parent of the new expression -- the variable declaration or assignment
            PsiElement parent = newExpression.getParent();

            // Traverse upwards to find a variable declaration or assignment
            while (parent != null) {

                //I If the parent is a variable declaration, return the variable
                if (parent instanceof PsiVariable) {
                    return (PsiVariable) parent; // Direct variable initialization

                    // Get the AssignmentExpression parent of the variable declaration
                } else if (parent instanceof PsiAssignmentExpression) {
                    PsiAssignmentExpression assignmentExpression = (PsiAssignmentExpression) parent;

                    // Get the left expression of the assignment
                    PsiExpression lhs = assignmentExpression.getLExpression();

                    // If the left expression is a reference expression, resolve it to get the variable associated with the assignment
                    if (lhs instanceof PsiReferenceExpression) {
                        PsiReferenceExpression referenceExpression = (PsiReferenceExpression) lhs;

                        PsiElement resolvedElement = referenceExpression.resolve();

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

        /**
         * Helper method to check if the file is in the current project.
         * It's used to check if the method call is in the current project (i.e defined by the user and not a library).
         *
         * @param file The PsiFile to check
         * @return A boolean indicating if the file is in the current project
         */
        private boolean isFileInCurrentProject(PsiFile file) {
            // Assuming `project` is an instance of com.intellij.openapi.project.Project
            Project project = file.getProject();
            return ProjectRootManager.getInstance(project).getFileIndex().isInContent(file.getVirtualFile());
        }
    }
}
