package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;


/**
 * This class checks for the usage of concrete classes to promote better design practices.
 *
 * <p>Two main cases are flagged:</p>
 *
 * <ul>
 *   <li><b>Case 1:</b> The class does not implement any interfaces or extend any classes (except java.lang.Object).
 *   <br>This is flagged because it is a standalone concrete class, which limits flexibility and testability.</li>
 *
 *   <li><b>Case 2:</b> The class implements interfaces or extends a superclass, but is directly used as a concrete type.
 *   <br>This is flagged because, even though the class is part of a hierarchy, using the concrete type directly bypasses the benefits of programming to an interface, such as flexibility and ease of testing.</li>
 * </ul>
 *
 * <p>Classes that only implement or extend Java utility classes/interfaces are not flagged, as they are considered part of the standard Java hierarchy.</p>
 */

public class ImplementationTypeCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     *
     * @param holder The holder for the problems found
     * @return The visitor for the inspection
     */
    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new ImplementationTypeVisitor(holder);
    }

    /**
     * This class extends the JavaElementVisitor to visit the elements in the code.
     * It checks if the variable type is a concrete class and if the class has implemented interfaces.
     * If both conditions are met, a problem is registered with the suggestion message to use interfaces instead of concrete classes.
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

        static {
            final String ruleName = "ImplementationTypeCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck() || RULE_CONFIG.getAntiPatternMessageMap().isEmpty();
        }

        /**
         * This method visits the variables in the code.
         * It checks if the variable type is a concrete class and if the class has implemented interfaces.
         * If both conditions are met, a problem is registered with the suggestion message to use interfaces instead of concrete classes.
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

            if (isConcreteClass(type)) {

                // Handle the concrete implementation type
                PsiClass psiClass = ((PsiClassType) type).resolve();

                if (psiClass != null && hasImplementedInterfaces(type)) {
                    holder.registerProblem(variable, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
                }
            }
        }

        /**
         * This method checks if the type is a concrete class.
         * This is done by checking if the type is a class and if the class is not an interface or abstract class.
         *
         * @param type The type to check
         * @return True if the type is a concrete class, false otherwise
         */
        private boolean isConcreteClass(PsiType type) {

            if (!(type instanceof PsiClassType)) {
                return false;
            }
            PsiClass psiClass = ((PsiClassType) type).resolve();

            if (psiClass == null) {
                return false;
            }

            // isInterface() returns true if the class is an interface
            // hasModifierProperty returns true if the class is abstract
            return !psiClass.isInterface() && !psiClass.hasModifierProperty(PsiModifier.ABSTRACT);
        }

        /**
         * This method checks if the class has implemented interfaces.
         * This is done by checking if the class implements interfaces or extends a class that is not
         * from the java.lang package.
         *
         * @param type The type to check
         * @return True if the class has implemented interfaces, false otherwise
         */
        private boolean hasImplementedInterfaces(PsiType type) {
            if (type instanceof PsiClassType) {
                PsiClass psiClass = ((PsiClassType) type).resolve();

                if (psiClass != null) {
                    // Exclude classes from java.lang package
                    if (psiClass.getQualifiedName() != null && (psiClass.getQualifiedName().startsWith("java.") || psiClass.getQualifiedName().startsWith("javax."))) {
                        return false;
                    }

                    PsiClass[] interfaces = psiClass.getInterfaces();
                    PsiClass superClass = psiClass.getSuperClass();

                    // Case 1: Class has no implemented interfaces and does not extend any class
                    if (interfaces.length == 0 && superClass != null && superClass.getQualifiedName().equals("java.lang.Object")) {
                        return true;
                    }

                    // Case 2: Class has implemented interfaces or extends a class that is not from java.lang package
                    if (interfaces.length > 0 || (superClass != null && !superClass.getQualifiedName().startsWith("java."))) {

                        for (PsiClass iface : interfaces) {

                            // If none of the interfaces are from java.lang or javax packages return true
                            String ifaceName = iface.getQualifiedName();
                            if (ifaceName != null && !ifaceName.startsWith("java.") && !ifaceName.startsWith("javax.")) {
                                return true;
                            }
                        }

                        // If the super class is not from java.lang or javax packages return true
                        if (superClass != null) {
                            String superClassName = superClass.getQualifiedName();
                            return superClassName != null && !superClassName.equals("java.lang.Object") && !superClassName.startsWith("java.") && !superClassName.startsWith("javax.");
                        }
                    }

                }
            }
            return false;
        }
    }
}
