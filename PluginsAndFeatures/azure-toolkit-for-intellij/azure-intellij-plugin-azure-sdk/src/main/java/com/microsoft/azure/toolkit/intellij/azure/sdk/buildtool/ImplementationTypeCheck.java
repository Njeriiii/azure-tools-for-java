package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;

/**
 * This class is an inspection tool that checks if a variable type is an Azure implementation type.
 * If the variable type is an Azure implementation type, or if the variable type extends or implements an Azure implementation type,
 * the inspection tool will flag it as a problem.
 */
public class ImplementationTypeCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     *
     * @param holder     The holder for the problems found
     * @param isOnTheFly Whether the inspection is being run on the fly - This is not used in this implementation, but is required by the interface
     * @return The visitor for the inspection
     */
    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new ImplementationTypeVisitor(holder);
    }

    /**
     * This class extends the JavaElementVisitor to visit the elements in the code.
     * It checks if the variable type is an Azure implementation type and flags it as a problem if it is.
     */
    static class ImplementationTypeVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;

        /**
         * Constructor for the visitor
         *
         * @param holder - the ProblemsHolder object to register the problem
         */
        ImplementationTypeVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;
        private static final String RULE_NAME = "ImplementationTypeCheck";

        static {
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(RULE_NAME);
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck() || RULE_CONFIG.getAntiPatternMessageMap().isEmpty() || RULE_CONFIG.getListedItemsToCheck().isEmpty();
        }

        /**
         * This method visits the variables in the code.
         * It checks if the variable type is an Azure implementation type and flags it as a problem if it is.
         */
        @Override
        public void visitVariable(@NotNull PsiVariable variable) {
            super.visitVariable(variable);

            if (SKIP_WHOLE_RULE) {
                return;
            }

            // Get the type of the variable - This is the type of the variable declaration
            // eg. List<String> myList = new ArrayList<>(); -> type = List<String>
            PsiType type = variable.getType();
            PsiClass psiClass = ((PsiClassType) type).resolve();

            // Check if the type directly used is an implementation type
            if (isImplementationType(psiClass) && variable.getNameIdentifier() != null) {
                holder.registerProblem(variable.getNameIdentifier(), RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));

                // Check if the type extends or implements an implementation type
            } else if (extendsOrImplementsImplementationType(psiClass) && variable.getNameIdentifier() != null) {
                holder.registerProblem(variable.getNameIdentifier(), RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
            }
        }

        /**
         * This method checks if the type is a class from the Azure package and if it is an implementation type.
         * It returns true if the type is an implementation type and false otherwise.
         */
        private boolean isImplementationType(PsiClass psiClass) {

            if (psiClass == null) {
                return false;
            }

            for (String listedItem : RULE_CONFIG.getListedItemsToCheck()) {
                if (psiClass.getQualifiedName() != null) {

                    // Check if the class is in the Azure package and if it is an implementation type
                    return psiClass.getQualifiedName().startsWith(RuleConfig.AZURE_PACKAGE_NAME) && psiClass.getQualifiedName().contains(listedItem);
                }
            }
            return false;
        }

        /**
         * This method checks if the type is a class that extends or implements an implementation type.
         * It returns true if the type extends or implements an implementation type and false otherwise.
         */
        private boolean extendsOrImplementsImplementationType(PsiClass psiClass) {

            // If the class is null or if it is a Java class, it is not an implementation type nor does it extend or implement one
            if (psiClass == null || psiClass.getQualifiedName().startsWith("java.")) {
                return false;
            }

            // Check if the current class is an implementation type
            if (isImplementationType(psiClass)) {
                return true;
            }

            // Check all direct interfaces
            for (PsiClass iface : psiClass.getInterfaces()) {
                if (extendsOrImplementsImplementationType(iface)) {
                    return true;
                }
            }
            // Check the direct superclass
            PsiClass superClass = psiClass.getSuperClass();
            if (superClass != null) {
                return extendsOrImplementsImplementationType(superClass);
            }
            return false;
        }
    }
}
