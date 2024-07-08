package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * This class extends the LocalInspectionTool and is used to inspect the usage of Azure Storage upload APIs in the code.
 * It checks if the upload methods are being called without a 'length' parameter of type 'long'.
 * The methods to check are defined in a JSON configuration file.
 * If such a method call is detected, a problem is registered with the ProblemsHolder.
 */

public class StorageUploadWithoutLengthCheck extends LocalInspectionTool {
    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Ensure Storage APIs use Length Parameter";
    }

    private static List<String> METHODS_TO_CHECK_LIST;
    private static final String RULE_NAME = "StorageUploadWithoutLengthCheck";
    private static final String METHODS_TO_CHECK_KEY = "methods_to_check";
    private static final String LENGTH_TYPE = "long";
    private static final Logger LOGGER = Logger.getLogger(StorageUploadWithoutLengthCheck.class.getName());



    static {
        try {
            METHODS_TO_CHECK_LIST = getMethodsToCheck();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading methods to check", e);
        }
    }

    // Get the list of methods to check from the configuration file
    private static List<String> getMethodsToCheck() throws IOException {

        JSONObject jsonObject = LoadJsonConfigFile.getInstance().getJsonObject();
        JSONArray methods = jsonObject.getJSONObject(RULE_NAME).getJSONArray(METHODS_TO_CHECK_KEY);
        return methods.toList().stream().map(Object::toString).collect(Collectors.toList());
    }

    /**
     * This method is used to build a PsiElementVisitor that will be used to visit the method calls in the code.
     *
     * @param holder
     * @param isOnTheFly
     * @return
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaRecursiveElementWalkingVisitor() {

            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                String methodName = expression.getMethodExpression().getReferenceName();

                if (!METHODS_TO_CHECK_LIST.contains(methodName)) {
                    return;
                }

                boolean hasLengthArg = false;

                PsiExpressionList argumentList = expression.getArgumentList();
                PsiExpression[] arguments = argumentList.getExpressions();

                for (PsiExpression arg : arguments) {
                    // Check if the argument is of type 'long'
                    if (arg.getType() != null && arg.getType().toString().equals(LENGTH_TYPE)) {
                        hasLengthArg = true;
                        break;
                    }
                    // Check if the argument is a method call
                    if (arg instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression argMethodCall = (PsiMethodCallExpression) arg;

                        // Analysing arguments that are method calls to check for 'long' type arguments
                        hasLengthArg = checkMethodCallChain(argMethodCall);
                    }
                }
                if (!hasLengthArg) {
                    holder.registerProblem(expression, "Azure Storage upload API without length parameter detected");
                }

            }
        };
    }

    /**
     * Analysing arguments that are chained method calls
     * This method is used to check if the method call chain has a constructor with 'long' type arguments.
     * The iteration starts from the end of the chain and goes up the chain.
     * The qualifier of the method call is checked for a constructor with 'long' type arguments.
     *
     * @param expression
     * @return boolean
     */
    public boolean checkMethodCallChain(PsiMethodCallExpression expression) {

        // Iterating up the chain of method calls
        PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();

        while (qualifier instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression qualifierMethodCall = (PsiMethodCallExpression) qualifier;

            qualifier = qualifierMethodCall.getMethodExpression().getQualifierExpression();

            // Checking for constructor with 'long' type arguments
            if (qualifier instanceof PsiNewExpression) {
                return isLengthArgumentInCall (qualifier);
            }
        }
        return false;
    }

    /**
     * This method is used to check if the constructor of the new expression has a 'long' type argument.
     *
     * @param qualifier - The qualifier of the method call
     * @return boolean
     */
    private boolean isLengthArgumentInCall (PsiExpression qualifier) {
        PsiNewExpression newExpression = (PsiNewExpression) qualifier;

        // Getting the arguments of the constructor
        PsiExpressionList newExpressionArgumentList = newExpression.getArgumentList();
        PsiExpression[] newExpressionArguments = newExpressionArgumentList.getExpressions();

        // Checking if the arguments are of type 'long'
        for (PsiExpression newExpressionArgument : newExpressionArguments) {
            if (newExpressionArgument.getType().toString().equals(LENGTH_TYPE)) {
                return true;
            }
        }
        return false;
    }
}