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
     * @param holder The holder for the problems found
     *@param isOnTheFly Whether the inspection is being run on the fly - This is not used in this implementation, but is required by the interface
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

            // Check if the type directly used is an implementation type
            if (isImplementationType(type) && variable.getNameIdentifier() != null) {
                holder.registerProblem(variable.getNameIdentifier(), RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));

                // Check if the type extends or implements an implementation type
            } else if (extendsOrImplementsImplementationType(type) && variable.getNameIdentifier() != null) {
                holder.registerProblem(variable.getNameIdentifier(), RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
            }
        }

        /**
         * This method checks if the type is a class from the Azure package and if it is an implementation type.
         * It returns true if the type is an implementation type and false otherwise.
         */
        private boolean isImplementationType(PsiType type) {

            if (!(type instanceof PsiClassType)) {
                return false;
            }
            PsiClass psiClass = ((PsiClassType) type).resolve();

            if (psiClass == null) {
                return false;
            }
            // Check if the class is in the Azure package and if it is an implementation type
            return psiClass.getQualifiedName().startsWith(RuleConfig.AZURE_PACKAGE_NAME) && psiClass.getQualifiedName().contains(RULE_CONFIG.getListedItemsToCheck().get(0));
        }

        /**
         * This method checks if the type is a class that extends or implements an implementation type.
         * It returns true if the type extends or implements an implementation type and false otherwise.
         */
        private boolean extendsOrImplementsImplementationType(PsiType type) {
            if (type instanceof PsiClassType) {
                PsiClass psiClass = ((PsiClassType) type).resolve();

                if (psiClass != null) {

                    PsiClass[] interfaces = psiClass.getInterfaces();
                    PsiClass superClass = psiClass.getSuperClass();

                    // Case 1: Class has no implemented interfaces and does not extend any class
                    if (interfaces.length == 0 && superClass != null && superClass.getQualifiedName().equals("java.lang.Object")) {
                        return false;
                    }

                    // Case 2: Class has implemented interfaces or extends a class that is an implementation type
                    if (interfaces.length > 0 || (superClass != null && !superClass.getQualifiedName().startsWith("java."))) {

                        for (PsiClass iface : interfaces) {

                            // If the interface is from the Azure package and is an implementation type return true
                            String ifaceName = iface.getQualifiedName();
                            return ifaceName != null && ifaceName.startsWith(RuleConfig.AZURE_PACKAGE_NAME) && ifaceName.contains(RULE_CONFIG.getListedItemsToCheck().get(0));
                        }

                        // If the super class is from the Azure package and is an implementation type return true
                        String superClassName = superClass.getQualifiedName();
                        return superClassName != null && superClassName.startsWith(RuleConfig.AZURE_PACKAGE_NAME) && superClassName.contains(RULE_CONFIG.getListedItemsToCheck().get(0));
                    }
                }
            }
            return false;
        }
    }
}
