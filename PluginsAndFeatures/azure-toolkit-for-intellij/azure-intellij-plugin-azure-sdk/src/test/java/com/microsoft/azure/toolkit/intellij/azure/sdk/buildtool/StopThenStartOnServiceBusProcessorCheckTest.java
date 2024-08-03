package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.StopThenStartOnServiceBusProcessorCheck.StopThenStartOnServiceBusProcessorVisitor;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class tests the StopThenStartOnServiceBusProcessorCheck class
 */
public class StopThenStartOnServiceBusProcessorCheckTest {

    // Create a mock ProblemsHolder to be used in the test
    @Mock
    private ProblemsHolder mockHolder;

    // Create a mock visitor for visiting the PsiMethodCallExpression
    @Mock
    private StopThenStartOnServiceBusProcessorVisitor mockVisitor;

    @BeforeEach
    public void setUp() {
        // Set up the test
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
    }

    /**
     * This test case tests the StopThenStartOnServiceBusProcessorCheck class when `stop` is called before `start`
     * on the same variable in the same method body without recursion.
     * The test case should register a problem with the ProblemsHolder.
     */
    @Test
    public void testSameVariableNonRecursiveStopThenStart() {
        String stopMethod = "stop";
        String startMethod = "start";
        String packageName = "com.azure.messaging.servicebus.ServiceBusProcessorClient";
        int numOfInvocations = 1;

        boolean sameVariable = true;
        boolean recursiveSecondLevel = false;
        boolean isStopThenStart = true;
        boolean directVariableInitialization = false;

        verifyRegisterProblem(stopMethod, startMethod, isStopThenStart, packageName, numOfInvocations, sameVariable, recursiveSecondLevel, directVariableInitialization);
    }

    /**
     * This test case tests the StopThenStartOnServiceBusProcessorCheck class when 'start' is not called after 'stop'
     * on the same variable in a different method body with recursion.
     * The test case should not register a problem with the ProblemsHolder.
     */
    @Test
    public void testSameVariableNonRecursiveStartThenStop() {
        String stopMethod = "stop";
        String startMethod = "start";
        String packageName = "com.azure.messaging.servicebus.ServiceBusProcessorClient";
        int numOfInvocations = 0;

        boolean sameVariable = true;
        boolean recursiveSecondLevel = true;
        boolean isStopThenStart = false;
        boolean directVariableInitialization = true;

        verifyRegisterProblem(stopMethod, startMethod, isStopThenStart, packageName, numOfInvocations, sameVariable, recursiveSecondLevel, directVariableInitialization);
    }

    /**
     * This test case tests the StopThenStartOnServiceBusProcessorCheck class when `stop` is called before `start`
     * on different variables in the same method body without recursion.
     * The test case should not register a problem with the ProblemsHolder.
     */
    @Test
    public void testDifferentVariablesNonRecursiveStopThenStart() {
        String stopMethod = "stop";
        String startMethod = "start";
        String packageName = "com.azure.messaging.servicebus.ServiceBusProcessorClient";
        int numOfInvocations = 0;

        boolean sameVariable = false;
        boolean recursiveSecondLevel = false;
        boolean isStopThenStart = true;
        boolean directVariableInitialization = false;

        verifyRegisterProblem(stopMethod, startMethod, isStopThenStart, packageName, numOfInvocations, sameVariable, recursiveSecondLevel, directVariableInitialization);
    }

    /**
     * This test case tests the StopThenStartOnServiceBusProcessorCheck class when 'start' is not called after 'stop'
     * on different variables in the same method body with recursion.
     * The test case should not register a problem with the ProblemsHolder.
     */
    @Test
    public void testDifferentVariablesNonRecursiveStartThenStop() {
        String stopMethod = "stop";
        String startMethod = "start";
        String packageName = "com.azure.messaging.servicebus.ServiceBusProcessorClient";
        int numOfInvocations = 0;

        boolean sameVariable = false;
        boolean recursiveSecondLevel = true;
        boolean isStopThenStart = false;
        boolean directVariableInitialization = true;

        verifyRegisterProblem(stopMethod, startMethod, isStopThenStart, packageName, numOfInvocations, sameVariable, recursiveSecondLevel, directVariableInitialization);
    }

    /**
     * This test case tests the StopThenStartOnServiceBusProcessorCheck class when `stop` is called before `start`
     * on the same variable in a recursive second level method.
     * The test case should register a problem with the ProblemsHolder.
     */
    @Test
    public void testSameVariableRecursiveStopThenStart() {
        String stopMethod = "stop";
        String startMethod = "start";
        String packageName = "com.azure.messaging.servicebus.ServiceBusProcessorClient";
        int numOfInvocations = 1;

        boolean sameVariable = true;
        boolean recursiveSecondLevel = true;
        boolean isStopThenStart = true;
        boolean directVariableInitialization = false;

        verifyRegisterProblem(stopMethod, startMethod, isStopThenStart, packageName, numOfInvocations, sameVariable, recursiveSecondLevel, directVariableInitialization);
    }

    /**
     * This test case tests the StopThenStartOnServiceBusProcessorCheck class when 'start' is not called after 'stop'
     * on the same variable in a recursive second level method.
     * The test case should not register a problem with the ProblemsHolder.
     */
    @Test
    public void testSameVariableRecursiveStartThenStop() {
        String stopMethod = "stop";
        String startMethod = "start";
        String packageName = "com.azure.messaging.servicebus.ServiceBusProcessorClient";
        int numOfInvocations = 0;

        boolean sameVariable = true;
        boolean recursiveSecondLevel = true;
        boolean isStopThenStart = false;
        boolean directVariableInitialization = true;

        verifyRegisterProblem(stopMethod, startMethod, isStopThenStart, packageName, numOfInvocations, sameVariable, recursiveSecondLevel, directVariableInitialization);
    }

    /**
     * This test case tests the StopThenStartOnServiceBusProcessorCheck class when `stop` is called before `start`
     * on different variables in a recursive second level method.
     * The test case should not register a problem with the ProblemsHolder.
     */
    @Test
    public void testDifferentVariablesRecursiveStopThenStart() {
        String stopMethod = "stop";
        String startMethod = "start";
        String packageName = "com.azure.messaging.servicebus.ServiceBusProcessorClient";
        int numOfInvocations = 0;

        boolean sameVariable = false;
        boolean recursiveSecondLevel = true;
        boolean isStopThenStart = true;
        boolean directVariableInitialization = false;

        verifyRegisterProblem(stopMethod, startMethod, isStopThenStart, packageName, numOfInvocations, sameVariable, recursiveSecondLevel, directVariableInitialization);
    }

    /**
     * This test case tests the StopThenStartOnServiceBusProcessorCheck class when 'start' is not called after 'stop'
     * on different variables in a recursive second level method.
     * The test case should not register a problem with the ProblemsHolder.
     */
    @Test
    public void testDifferentVariablesRecursiveStartThenStop() {
        String stopMethod = "stop";
        String startMethod = "start";
        String packageName = "com.azure.messaging.servicebus.ServiceBusProcessorClient";
        int numOfInvocations = 0;

        boolean sameVariable = false;
        boolean recursiveSecondLevel = true;
        boolean isStopThenStart = false;
        boolean directVariableInitialization = true;

        verifyRegisterProblem(stopMethod, startMethod, isStopThenStart, packageName, numOfInvocations, sameVariable, recursiveSecondLevel, directVariableInitialization);
    }

    /**
     * This helper method creates a new StopThenStartOnServiceBusProcessorVisitor object
     *
     * @return a new StopThenStartOnServiceBusProcessorVisitor object
     */
    private StopThenStartOnServiceBusProcessorVisitor createVisitor() {
        return new StopThenStartOnServiceBusProcessorVisitor(mockHolder);
    }

    /**
     * This helper method verifies that the ProblemsHolder has been registered with the problem
     *
     * @param stopMethod       the stop method to be called
     * @param startMethod      the start method to be called
     * @param packageName      the package name of the variable
     * @param numOfInvocations the number of times the problem should be registered
     * @param sameVariable     a boolean indicating if the start method is called on the same variable
     */
    private void verifyRegisterProblem(String stopMethod, String startMethod, boolean isStopThenStart, String packageName, int numOfInvocations, boolean sameVariable, boolean recursiveSecondLevel, boolean directVariableInitialization) {

        PsiNewExpression newExpression = mock(PsiNewExpression.class);
        PsiMethod method = mock(PsiMethod.class);
        PsiCodeBlock mainBody = mock(PsiCodeBlock.class);
        PsiCodeBlock secondBody = mock(PsiCodeBlock.class);

        when(newExpression.getParent()).thenReturn(method);

        if (recursiveSecondLevel) {
            when(method.getBody()).thenReturn(mainBody);
        } else {
            when(method.getBody()).thenReturn(secondBody);
        }

        // findAssociatedVariable method
        PsiVariable findAssociatedVariable = mock(PsiVariable.class);
        PsiAssignmentExpression assignmentExpression = mock(PsiAssignmentExpression.class);
        PsiReferenceExpression referenceExpression = mock(PsiReferenceExpression.class);

        // isServiceBusProcessorClient method
        PsiType type = mock(PsiType.class);

        // visitMethodBody method
        PsiExpressionStatement stopChild = mock(PsiExpressionStatement.class);
        PsiExpressionStatement startChild = mock(PsiExpressionStatement.class);

        PsiElement[] stopStartChildren;
        if (isStopThenStart) {
            stopStartChildren = new PsiElement[]{stopChild, startChild};
        } else {
            stopStartChildren = new PsiElement[]{startChild, stopChild};
        }

        PsiMethodCallExpression stopExpression = mock(PsiMethodCallExpression.class);
        PsiMethodCallExpression startExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression stopMethodExpression = mock(PsiReferenceExpression.class);
        PsiReferenceExpression startMethodExpression = mock(PsiReferenceExpression.class);
        PsiReferenceExpression stopQualifierExpression = mock(PsiReferenceExpression.class);
        PsiReferenceExpression startQualifierExpression = mock(PsiReferenceExpression.class);

        PsiVariable stopResolvedElement = mock(PsiVariable.class);
        PsiVariable startResolvedElement = mock(PsiVariable.class);

        PsiMethodCallExpression helperExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression helperMethodExpression = mock(PsiReferenceExpression.class);
        PsiReferenceExpression helperQualifierExpression = mock(PsiReferenceExpression.class);

        PsiMethod helperMethod = mock(PsiMethod.class);
        PsiFile containingFile = mock(PsiFile.class);
        Project project = mock(Project.class);
        ProjectRootManager projectRootManager = mock(ProjectRootManager.class);
        ProjectFileIndex projectFileIndex = mock(ProjectFileIndex.class);
        PsiExpressionStatement otherChild = mock(PsiExpressionStatement.class);
        PsiElement[] otherChildren = new PsiElement[]{otherChild};

        // findAssociatedVariable method
        if (directVariableInitialization) {
            when(newExpression.getParent()).thenReturn(findAssociatedVariable);
        } else {

            when(newExpression.getParent()).thenReturn(assignmentExpression);
            when(assignmentExpression.getLExpression()).thenReturn(referenceExpression);
            when(referenceExpression.resolve()).thenReturn(findAssociatedVariable);
        }

        // isServiceBusProcessorClient method
        when(findAssociatedVariable.getType()).thenReturn(type);
        when(type.getCanonicalText()).thenReturn(packageName);

        // visitMethodBody method & checkMethodCall method
        when(secondBody.getChildren()).thenReturn(stopStartChildren);

        when(stopChild.getExpression()).thenReturn(stopExpression);
        when(startChild.getExpression()).thenReturn(startExpression);

        when(stopExpression.getMethodExpression()).thenReturn(stopMethodExpression);
        when(startExpression.getMethodExpression()).thenReturn(startMethodExpression);
        when(stopMethodExpression.getQualifierExpression()).thenReturn(stopQualifierExpression);
        when(startMethodExpression.getQualifierExpression()).thenReturn(startQualifierExpression);

        if (sameVariable) {
            when(stopQualifierExpression.resolve()).thenReturn(findAssociatedVariable);
            when(startQualifierExpression.resolve()).thenReturn(findAssociatedVariable);
        } else {
            when(stopQualifierExpression.resolve()).thenReturn(stopResolvedElement);
            when(startQualifierExpression.resolve()).thenReturn(startResolvedElement);
        }

        when(stopMethodExpression.getReferenceName()).thenReturn(stopMethod);
        when(startMethodExpression.getReferenceName()).thenReturn(startMethod);

        when(mainBody.getChildren()).thenReturn(otherChildren);
        when(otherChild.getExpression()).thenReturn(helperExpression);
        when(helperExpression.getMethodExpression()).thenReturn(helperMethodExpression);
        when(helperMethodExpression.getQualifierExpression()).thenReturn(helperQualifierExpression);
        when(helperQualifierExpression.resolve()).thenReturn(null);

        when(helperExpression.resolveMethod()).thenReturn(helperMethod);

        // isFileInCurrentProject method
        when(helperMethod.getContainingFile()).thenReturn(containingFile);
        when(containingFile.getProject()).thenReturn(project);
        when(projectRootManager.getInstance(project)).thenReturn(projectRootManager);
        when(projectRootManager.getFileIndex()).thenReturn(projectFileIndex);
        when(projectFileIndex.isInContent(containingFile.getVirtualFile())).thenReturn(true);

        when(helperMethod.getBody()).thenReturn(secondBody);

        mockVisitor.visitElement(newExpression);
        mockVisitor.visitMethod(method);

        // Verify that the ProblemsHolder has been registered with the problem
        verify(mockHolder, times(numOfInvocations)).registerProblem(eq(startExpression), contains("Starting Processor that was stopped before is not recommended, and this feature may be deprecated in the future. Please close this processor instance and create a new one to restart processing"));
    }
}
